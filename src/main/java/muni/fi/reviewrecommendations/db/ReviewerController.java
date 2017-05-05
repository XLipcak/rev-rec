package muni.fi.reviewrecommendations.db;

import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import muni.fi.reviewrecommendations.common.GerritBrowser;
import muni.fi.reviewrecommendations.db.model.filePath.FilePath;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequest;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestService;
import muni.fi.reviewrecommendations.db.model.reviewer.Developer;
import muni.fi.reviewrecommendations.recommendationTechniques.ReviewerRecommendationService;
import muni.fi.reviewrecommendations.recommendationTechniques.revfinder.RevFinder;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private PullRequestService pullRequestService;

    @Autowired
    private ReviewerRecommendationService reviewerRecommendationService;

    @RequestMapping(value = "/reviewer", method = RequestMethod.GET)
    List<Developer> all(@RequestParam(value = "gerritChangeNumber", required = true) String gerritChangeNumber) throws IOException, RestApiException {
        GerritBrowser gerritBrowser = new GerritBrowser("https://android-review.googlesource.com");
        //GitBrowser gitBrowser = new GitBrowser("sdk", false);
        String projectName = "Android";

        //String changeId = pullRequestDAO.findByChangeNumber(new Integer(gerritChangeNumber)).get(0).getChangeId();
        PullRequest pullRequest = new PullRequest();

        Set<FilePath> result = new HashSet(gerritBrowser.getFilePaths(gerritChangeNumber));
        pullRequest.setFilePaths(result);

        ChangeInfo changeInfo = gerritBrowser.getChange(gerritChangeNumber);
        RevFinder revFinder = new RevFinder(pullRequestService.getAllPreviousReviews(changeInfo.created.getTime(), projectName));
        //ReviewBot reviewBot = new ReviewBot(gitBrowser, gerritBrowser);

        return reviewerRecommendationService.recommend(revFinder, pullRequest);
    }
}
