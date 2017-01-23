package muni.fi.reviewrecommendations.db;

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.restapi.RestApiException;
import muni.fi.reviewrecommendations.InitialLoader;
import muni.fi.reviewrecommendations.common.GerritBrowser;
import muni.fi.reviewrecommendations.common.GitBrowser;
import muni.fi.reviewrecommendations.db.model.filePath.FilePath;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequest;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestDAO;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import muni.fi.reviewrecommendations.recommendationTechniques.ReviewerRecommendation;
import muni.fi.reviewrecommendations.recommendationTechniques.revfinder.RevFinder;
import muni.fi.reviewrecommendations.recommendationTechniques.revfinder.Review;
import muni.fi.reviewrecommendations.recommendationTechniques.reviewbot.ReviewBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

/**
 * Created by Kubo on 9.1.2017.
 */
@RestController
@RequestMapping(path = "/api")
public class ReviewerController {

    @Autowired
    private PullRequestDAO pullRequestDAO;

    @RequestMapping(value = "/reviewer", method = RequestMethod.GET)
    List<Reviewer> all(@RequestParam(value = "gerritChangeNumber", required = true) String gerritChangeNumber) throws IOException, RestApiException {
        GerritBrowser gerritBrowser = new GerritBrowser("https://android-review.googlesource.com");
        GitBrowser gitBrowser = new GitBrowser("sdk", false);

        //String changeId = pullRequestDAO.findByChangeNumber(new Integer(gerritChangeNumber)).get(0).getChangeId();
        Review review = new Review();
        review.setFilePaths(gerritBrowser.getFilePaths(gerritChangeNumber));

        Long timeStamp = gerritBrowser.getChange(gerritChangeNumber).created.getTime();
        RevFinder revFinder = new RevFinder(getAllPreviousReviews(timeStamp));
        ReviewBot reviewBot = new ReviewBot(gitBrowser, gerritBrowser);

        return recommend(revFinder, review);
    }

    private static List<Reviewer> recommend(ReviewerRecommendation reviewerRecommendation, Review review) {
        Map<Reviewer, Double> map = reviewerRecommendation.reviewersRankingAlgorithm(review);
        map = InitialLoader.sortByValue(map);

        List<Reviewer> result = new ArrayList<>();
        for (Map.Entry<Reviewer, Double> entry : map.entrySet()) {
            System.out.println(entry.getKey().getName() + " => " + entry.getValue());
            result.add(entry.getKey());
        }

        System.out.println("Done!");

        return result;
    }

    private List<Review> getAllPreviousReviews(Long timeStamp) {

        List<Review> allPreviousReviews = new ArrayList<>();

        Set<PullRequest> pullRequests = new HashSet<>(Lists.newArrayList(pullRequestDAO.findByTimeLessThan(timeStamp)));

        for (PullRequest pullRequest : pullRequests) {
            Review review = new Review();
            review.setDate(new Date(pullRequest.getTime() * 1000));

            List<String> filePaths = new ArrayList<>();
            for (FilePath filePath : pullRequest.getFilePaths()) {
                filePaths.add(filePath.getFilePath());
            }
            review.setFilePaths(filePaths);
            review.setReviewers(new ArrayList<>(pullRequest.getCodeReviewers()));

            allPreviousReviews.add(review);
        }
        return allPreviousReviews;
    }
}
