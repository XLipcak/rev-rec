package muni.fi.reviewrecommendations;

import com.google.gerrit.extensions.restapi.RestApiException;
import muni.fi.reviewrecommendations.common.GerritBrowser;
import muni.fi.reviewrecommendations.db.DataLoader;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequest;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestDAO;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestService;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import muni.fi.reviewrecommendations.recommendationTechniques.ReviewerRecommendationService;
import muni.fi.reviewrecommendations.recommendationTechniques.bayes.BayesRec;
import muni.fi.reviewrecommendations.recommendationTechniques.revfinder.RevFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is used to execute the functionality of the project.
 *
 * @author Jakub Lipcak, Masaryk University
 */
@Component
public class InitialLoader implements CommandLineRunner {


    @Autowired
    private DataLoader dataLoader;

    @Autowired
    private PullRequestService pullRequestService;

    @Autowired
    private ReviewerRecommendationService reviewerRecommendationService;

    @Autowired
    private PullRequestDAO pullRequestDAO;

    @Autowired
    private BayesRec bayesRec;

    @Value("${recommendation.project}")
    private String project;

    @Override
    public void run(String... strings) throws Exception {
        //GerritBrowser gerritBrowser = new GerritBrowser("https://android-review.googlesource.com");
        //GitBrowser gitBrowser = new GitBrowser("sdk", false);
        GerritBrowser gerritBrowser = new GerritBrowser("https://codereview.qt-project.org");


        //GerritBrowser gerritBrowser = new GerritBrowser("https://review.openstack.org/");
        //dataLoader.loadDataFromFile("data/openstack.json", gerritBrowser, "openstack");
        //dataLoader.loadDataByChangeIdsFromFile("data/openstack.json", gerritBrowser, "openstack");


        //GerritBrowser gerritBrowser = new GerritBrowser("https://

        //dataLoader.loadDataByChangeIdsFromFile("data/qt.json", gerritBrowser, "qt");

        testTechniqueRevFinder(project);

        //testTechniqueBayes(project);
    }

    private void testTechniqueRevFinder(String projectName) throws IOException, RestApiException {

        int top1Counter = 0;
        int top3Counter = 0;
        int top5Counter = 0;
        int top10Counter = 0;
        double mrrValue = 0;

        int iterationsCounter = 0;
        List<PullRequest> pullRequests = pullRequestService.findByProjectNameOrderByTimeDesc(projectName);
        List<PullRequest> reviewList = pullRequests.subList(iterationsCounter, pullRequests.size());
        for (PullRequest pullRequest : pullRequests) {
            iterationsCounter++;
            if (reviewList.size() == 1) {
                break;
            }
            reviewList = reviewList.subList(1, reviewList.size());
            //List<Review> reviews = pullRequestService.getAllPreviousReviews(pullRequest.getTime(), projectName);
            RevFinder revFinder = new RevFinder(//reviewList
                    pullRequestService.getAllPreviousReviews(pullRequest.getTime(), projectName));


            List<Reviewer> reviewers = reviewerRecommendationService.recommend(revFinder, pullRequest);

            printData(iterationsCounter + " " + pullRequest.getChangeNumber());

            for (int x = 0; x < 1 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top1Counter++;
                    break;
                }
            }

            for (int x = 0; x < 3 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top3Counter++;
                    break;
                }
            }

            for (int x = 0; x < 5 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top5Counter++;
                    break;
                }
            }

            for (int x = 0; x < 10 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top10Counter++;
                    break;
                }
            }

            //mrr
            for (int x = 0; x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    mrrValue += 1d / (x + 1);
                    break;
                }
            }

            printData("Top-1 accuracy: " + ((double) top1Counter / (double) iterationsCounter) * 100d + "%");
            printData("Top-3 accuracy: " + ((double) top3Counter / (double) iterationsCounter) * 100d + "%");
            printData("Top-5 accuracy: " + ((double) top5Counter / (double) iterationsCounter) * 100d + "%");
            printData("Top-10 accuracy: " + ((double) top10Counter / (double) iterationsCounter) * 100d + "%");
            printData("Mean Reciprocal Rank: " + mrrValue / iterationsCounter);
        }

        printData("Top-1 accuracy: " + ((double) top1Counter / (double) iterationsCounter) * 100d + "%");
        printData("Top-3 accuracy: " + ((double) top3Counter / (double) iterationsCounter) * 100d + "%");
        printData("Top-5 accuracy: " + ((double) top5Counter / (double) iterationsCounter) * 100d + "%");
        printData("Top-10 accuracy: " + ((double) top10Counter / (double) iterationsCounter) * 100d + "%");
        printData("Mean Reciprocal Rank: " + mrrValue / iterationsCounter);
    }

    private void testTechniqueBayes(String projectName) throws IOException, RestApiException {

        BayesRec reviewerRecommendation = bayesRec;
        int top1Counter = 0;
        int top3Counter = 0;
        int top5Counter = 0;
        int top10Counter = 0;
        double mrrValue = 0;

        int iterationsCounter = 0;
        List<PullRequest> pullRequests = pullRequestService.findByProjectNameOrderByTimeDesc(projectName);

        for (PullRequest pullRequest : pullRequests) {

            /*List<Review> reviews = pullRequestService.getAllPreviousReviews(pullRequest.getTime(), projectName);
            RevFinder revFinder = new RevFinder(pullRequestService.getAllPreviousReviews(pullRequest.getTime(), projectName));*/
            if (iterationsCounter % 500 == 0) {
                if (iterationsCounter + 500 < pullRequests.size()) {
                    reviewerRecommendation.buildModel(pullRequests.get(iterationsCounter + 500).getTime());
                }
            }


            List<Reviewer> reviewers = reviewerRecommendationService.recommend(reviewerRecommendation, pullRequest);
            //List<Reviewer> reviewers = reviewerRecommendationService.recommend(reviewerRecommendation, revFinder, review);
            /*List<Reviewer> reviewers = removeSelfReviewers(removeRetiredReviewers(pullRequest, 31104000000l, reviewerRecommendationService.recommend(revFinder, review), projectName),
                    pullRequest, findSelfReviewers(projectName)); //12 months*/

            iterationsCounter++;
            printData(iterationsCounter + " " + pullRequest.getChangeNumber());

            for (int x = 0; x < 1 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top1Counter++;
                    break;
                }
            }

            for (int x = 0; x < 3 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top3Counter++;
                    break;
                }
            }

            for (int x = 0; x < 5 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top5Counter++;
                    break;
                }
            }

            for (int x = 0; x < 10 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top10Counter++;
                    break;
                }
            }

            //mrr
            for (int x = 0; x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    mrrValue += 1d / (x + 1);
                    break;
                }
            }

            printData("Top-1 accuracy: " + ((double) top1Counter / (double) iterationsCounter) * 100d + "%");
            printData("Top-3 accuracy: " + ((double) top3Counter / (double) iterationsCounter) * 100d + "%");
            printData("Top-5 accuracy: " + ((double) top5Counter / (double) iterationsCounter) * 100d + "%");
            printData("Top-10 accuracy: " + ((double) top10Counter / (double) iterationsCounter) * 100d + "%");
            printData("Mean Reciprocal Rank: " + mrrValue / iterationsCounter);
        }

        printData("Top-1 accuracy: " + ((double) top1Counter / (double) iterationsCounter) * 100d + "%");
        printData("Top-3 accuracy: " + ((double) top3Counter / (double) iterationsCounter) * 100d + "%");
        printData("Top-5 accuracy: " + ((double) top5Counter / (double) iterationsCounter) * 100d + "%");
        printData("Top-10 accuracy: " + ((double) top10Counter / (double) iterationsCounter) * 100d + "%");
        printData("Mean Reciprocal Rank: " + mrrValue / iterationsCounter);
    }

    Set<Reviewer> mergeReviewers(PullRequest pullRequest, Set<Reviewer> reviewersWithAtLeastOneReview) {
        Set<Reviewer> result = new HashSet<>();

        for (Reviewer reviewer : pullRequest.getReviewers()) {
            if (reviewersWithAtLeastOneReview.contains(reviewer)) {
                result.add(reviewer);
            }
        }

        if (reviewersWithAtLeastOneReview.contains(pullRequest.getOwner())) {
            result.add(pullRequest.getOwner());
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
            for (Reviewer reviewer : pullRequest.getReviewers()) {
                if (pullRequest.getOwner().equals(reviewer)) {
                    result.add(reviewer);
                }
            }
        }
        return result;
    }

    private String removeSlash(String filePath) {
        String result = "";

        for (int x = 0; x < filePath.length(); x++) {
            if (filePath.charAt(x) != '/') {
                result += filePath.charAt(x);
            }
        }
        return result;
    }

    private void printData(String text) {
        try {
            System.out.println(text);
            FileWriter fw = new FileWriter("output/" + project + ".txt", true);
            fw.write(text + "\n");
            fw.close();
        } catch (IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }
}