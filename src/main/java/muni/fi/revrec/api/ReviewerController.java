package muni.fi.revrec.api;

import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import muni.fi.revrec.common.GerritBrowser;
import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.project.Project;
import muni.fi.revrec.model.project.ProjectDAO;
import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.reviewer.Developer;
import muni.fi.revrec.recommendation.bayesrec.BayesRec;
import muni.fi.revrec.recommendation.revfinder.RevFinder;
import muni.fi.revrec.recommendation.reviewbot.ReviewBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * REST interface of the system.
 *
 * @author Jakub Lipcak, Masaryk University
 */
@RestController
@RequestMapping(path = "/api")
public class ReviewerController {

    private enum RecommendationMethod {
        REVIEWBOT, REVFINDER, BAYES
    }

    @Value("${recommendation.project}")
    private String projectName;

    @Autowired
    private ProjectDAO projectDAO;

    @Autowired
    private RevFinder revFinder;

    @Autowired
    private ReviewBot reviewBot;

    @Autowired
    private BayesRec bayesRec;

    @Autowired
    private GerritBrowser gerritBrowser;

    @RequestMapping(value = "/reviewers-recommendation", method = RequestMethod.GET)
    List<Developer> all(@RequestParam(value = "gerritChangeNumber", required = true) String gerritChangeNumber,
                        @RequestParam(value = "recommendationMethod", required = false) RecommendationMethod recommendationMethod) throws IOException, RestApiException {
        Project project = projectDAO.findOne(projectName);

        PullRequest pullRequest = createPullRequest(gerritChangeNumber, gerritBrowser);

        if (recommendationMethod != null) {
            switch (recommendationMethod) {
                case REVIEWBOT:
                    return reviewBot.recommend(pullRequest);
                case REVFINDER:
                    return revFinder.recommend(pullRequest);
                case BAYES:
                    return bayesRec.recommend(pullRequest);
            }
        }

        return revFinder.recommend(pullRequest);
    }

    private PullRequest createPullRequest(String gerritChangeNumber, GerritBrowser gerritBrowser) throws RestApiException {
        ChangeInfo changeInfo = gerritBrowser.getChange(gerritChangeNumber);
        PullRequest pullRequest = new PullRequest();
        pullRequest.setTimestamp(changeInfo.created.getTime());
        Set<FilePath> result = new HashSet<>(gerritBrowser.getFilePaths(gerritChangeNumber));
        pullRequest.setFilePaths(result);

        return pullRequest;
    }
}
