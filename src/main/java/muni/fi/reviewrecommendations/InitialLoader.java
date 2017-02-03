package muni.fi.reviewrecommendations;

import com.google.gerrit.extensions.restapi.RestApiException;
import muni.fi.reviewrecommendations.common.GerritBrowser;
import muni.fi.reviewrecommendations.db.DataLoader;
import muni.fi.reviewrecommendations.db.model.filePath.FilePath;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequest;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestService;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import muni.fi.reviewrecommendations.recommendationTechniques.Review;
import muni.fi.reviewrecommendations.recommendationTechniques.ReviewerRecommendationService;
import muni.fi.reviewrecommendations.recommendationTechniques.revfinder.RevFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Kubo on 8.1.2017.
 */
@Component
public class InitialLoader implements CommandLineRunner {


    @Autowired
    private DataLoader dataLoader;

    @Autowired
    private PullRequestService pullRequestService;

    @Autowired
    private ReviewerRecommendationService reviewerRecommendationService;

    @Override
    public void run(String... strings) throws Exception {
        String projectName = "Android";
        GerritBrowser gerritBrowser = new GerritBrowser("https://android-review.googlesource.com");
        //GitBrowser gitBrowser = new GitBrowser("sdk", false);

        //dataLoader.loadDataByChangeIdsFromFile("android.json", gerritBrowser, "Android");

        testTechnique(100, projectName);
    }

    private void testTechnique(int iterations, String projectName) throws IOException, RestApiException {

        int top1Counter = 0;
        int top3Counter = 0;
        int top5Counter = 0;
        int top10Counter = 0;

        int iterationsCounter = 0;
        for (PullRequest pullRequest : pullRequestService.findByProjectName(projectName)) {
            iterationsCounter++;
            System.out.println(iterationsCounter + " " + pullRequest.getChangeNumber());
            if (iterationsCounter > iterations) {
                break;
            }
            RevFinder revFinder = new RevFinder(pullRequestService.getAllPreviousReviews(pullRequest.getChangeId(), projectName));
            Review review = new Review();
            List<String> paths = new ArrayList<>();
            for (FilePath filePath : pullRequest.getFilePaths()) {
                paths.add(pullRequest.getSubProject().hashCode() + "/" + filePath.getFilePath());
            }
            review.setFilePaths(paths);

            //List<Reviewer> reviewers = recommend(revFinder, review);
            List<Reviewer> reviewers = removeSelfReviewers(removeRetiredReviewers(pullRequest, 31104000000l, reviewerRecommendationService.recommend(revFinder, review), projectName),
                    pullRequest, findSelfReviewers(projectName)); //12 months

            for (int x = 0; x < 1 && x < reviewers.size(); x++) {
                if (pullRequest.getAllSpecificCodeReviewers().contains(reviewers.get(x))) {
                    top1Counter++;
                    break;
                }
            }

            for (int x = 0; x < 3 && x < reviewers.size(); x++) {
                if (pullRequest.getAllSpecificCodeReviewers().contains(reviewers.get(x))) {
                    top3Counter++;
                    break;
                }
            }

            for (int x = 0; x < 5 && x < reviewers.size(); x++) {
                if (pullRequest.getAllSpecificCodeReviewers().contains(reviewers.get(x))) {
                    top5Counter++;
                    break;
                }
            }

            for (int x = 0; x < 10 && x < reviewers.size(); x++) {
                if (pullRequest.getAllSpecificCodeReviewers().contains(reviewers.get(x))) {
                    top10Counter++;
                    break;
                }
            }

            System.out.println("Top-1 accuracy: " + ((double) top1Counter / (double) iterationsCounter) * 100d + "%");
            System.out.println("Top-3 accuracy: " + ((double) top3Counter / (double) iterationsCounter) * 100d + "%");
            System.out.println("Top-5 accuracy: " + ((double) top5Counter / (double) iterationsCounter) * 100d + "%");
            System.out.println("Top-10 accuracy: " + ((double) top10Counter / (double) iterationsCounter) * 100d + "%");
        }

        System.out.println("Top-1 accuracy: " + ((double) top1Counter / (double) iterations) * 100d + "%");
        System.out.println("Top-3 accuracy: " + ((double) top3Counter / (double) iterations) * 100d + "%");
        System.out.println("Top-5 accuracy: " + ((double) top5Counter / (double) iterations) * 100d + "%");
        System.out.println("Top-10 accuracy: " + ((double) top10Counter / (double) iterations) * 100d + "%");
    }

    Set<Reviewer> mergeReviewers(PullRequest pullRequest, Set<Reviewer> reviewersWithAtLeastOneReview) {
        Set<Reviewer> result = new HashSet<>();

        for (Reviewer reviewer : pullRequest.getCodeReviewers()) {
            if (reviewersWithAtLeastOneReview.contains(reviewer)) {
                result.add(reviewer);
            }
        }

        if (reviewersWithAtLeastOneReview.contains(pullRequest.getOwner())) {
            result.add(pullRequest.getOwner());
        }

        return result;
    }

    private List<Reviewer> removeRetiredReviewers(PullRequest pullRequest, long timeRetired, List<Reviewer> reviewersList, String projectName) {
        List<Reviewer> result = new ArrayList<>();
        for (Reviewer reviewer : reviewersList) {
            if (pullRequestService.findByCodeReviewersAndTimeLessThanAndTimeGreaterThanAndProjectName(reviewer, pullRequest.getTime(), pullRequest.getTime() - timeRetired, projectName).size() > 0) {
                result.add(reviewer);
                if (result.size() > 15) {
                    return result;
                }
            }
        }

        return result;
    }

    private List<Reviewer> removeSelfReviewers(List<Reviewer> reviewersList, PullRequest pullRequest, Set<Reviewer> selfReviewers) {
        List<Reviewer> result = new ArrayList<>();
        for (Reviewer reviewer : reviewersList) {
            if (pullRequest.getOwner().equals(reviewer) && !selfReviewers.contains(reviewer)) {
                System.out.println("Self reviewer removed!");
                //is recommended, but never did self review before
            } else {
                result.add(reviewer);
            }
        }
        return result;
    }

    private Set<Reviewer> findSelfReviewers(String projectName) {
        Set<Reviewer> result = new HashSet<>();
        for (PullRequest pullRequest : pullRequestService.findByProjectName(projectName)) {
            for (Reviewer reviewer : pullRequest.getCodeReviewers()) {
                if (pullRequest.getOwner().equals(reviewer)) {
                    result.add(reviewer);
                }
            }
        }
        return result;
    }
}