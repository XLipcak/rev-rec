package muni.fi.revrec;

import com.google.gerrit.extensions.restapi.RestApiException;
import muni.fi.revrec.common.GerritService;
import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.pullRequest.PullRequestDAO;
import muni.fi.revrec.model.reviewer.Developer;
import muni.fi.revrec.recommendation.bayesrec.BayesRec;
import muni.fi.revrec.recommendation.revfinder.RevFinder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * This class is used to demonstrate the functionality of the project. Run method will always be executed
 * after the deploy of the application.
 *
 * @author Jakub Lipcak, Masaryk University
 */
@Component
public class InitialLoader implements CommandLineRunner {

    private final Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private PullRequestDAO pullRequestDAO;

    @Autowired
    private GerritService gerritService;

    @Autowired
    private BayesRec bayesRec;

    @Autowired
    private RevFinder revFinder;

    @Value("${recommendation.project}")
    private String project;

    @Override
    public void run(String... strings) throws Exception {

        //testTechniqueRevFinder();
        testTechniqueBayes();

//        printLine("");
//        ObjectMapper mapper = new ObjectMapper();
//        List<PullRequest> pullRequests = pullRequestDAO.findByProjectNameOrderByTimeDesc(project);
//        mapper.writeValue(new File("openstack.json"), pullRequests);
    }

    /**
     * Test RevFinder algorithm.
     *
     * @throws IOException
     * @throws RestApiException
     */
    private void testTechniqueRevFinder() throws IOException, RestApiException {

        // init variables
        int top1Counter = 0;
        int top3Counter = 0;
        int top5Counter = 0;
        int top10Counter = 0;
        double mrrValue = 0;
        int iterationsCounter = 0;
        List<PullRequest> pullRequests = pullRequestDAO.findByProjectNameOrderByTimestampDesc(project);

        for (PullRequest pullRequest : pullRequests) {

            // ensure 11 folds testing setup
            if (revFinder.getAllPreviousReviews().size() < pullRequests.size() / 11) {
                break;
            }
            iterationsCounter++;

            // Remove actual pull request from the review list, what ensures that only
            // previous reviews will be used for the recommendation.
            revFinder.setAllPreviousReviews(revFinder.getAllPreviousReviews().subList(1, revFinder.getAllPreviousReviews().size()));

            List<Developer> reviewers = revFinder.recommend(pullRequest);

            // Top-1 accuracy
            for (int x = 0; x < 1 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewer().contains(reviewers.get(x))) {
                    top1Counter++;
                    break;
                }
            }

            // Top-3 accuracy
            for (int x = 0; x < 3 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewer().contains(reviewers.get(x))) {
                    top3Counter++;
                    break;
                }
            }

            // Top-5 accuracy
            for (int x = 0; x < 5 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewer().contains(reviewers.get(x))) {
                    top5Counter++;
                    break;
                }
            }

            // Top-10 accuracy
            for (int x = 0; x < 10 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewer().contains(reviewers.get(x))) {
                    top10Counter++;
                    break;
                }
            }

            // Mean Reciprocal Rank
            for (int x = 0; x < reviewers.size(); x++) {
                if (pullRequest.getReviewer().contains(reviewers.get(x))) {
                    mrrValue += 1d / (x + 1);
                    break;
                }
            }

            printMetrics(iterationsCounter, top1Counter, top3Counter, top5Counter, top10Counter, mrrValue, pullRequest.getChangeNumber());
        }
    }

    private void testTechniqueBayes() throws IOException, RestApiException {

        // init variables
        int top1Counter = 0;
        int top3Counter = 0;
        int top5Counter = 0;
        int top10Counter = 0;
        double mrrValue = 0;
        int iterationsCounter = 0;
        List<PullRequest> pullRequests = pullRequestDAO.findByProjectNameOrderByTimestampDesc(project);
        int foldSize = pullRequests.size() / 11;
        int modelBuildCounter = 0;

        for (PullRequest pullRequest : pullRequests) {

            // ensure 11 folds testing setup
            if (iterationsCounter % foldSize == 0) {
                if (modelBuildCounter < 10) {
                    bayesRec.buildModel(pullRequests.get(iterationsCounter + foldSize).getTimestamp());
                    modelBuildCounter++;
                } else {
                    break;
                }
            }
            iterationsCounter++;

            List<Developer> reviewers = bayesRec.recommend(pullRequest);

            // Top-1 accuracy
            for (int x = 0; x < 1 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewer().contains(reviewers.get(x))) {
                    top1Counter++;
                    break;
                }
            }

            // Top-3 accuracy
            for (int x = 0; x < 3 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewer().contains(reviewers.get(x))) {
                    top3Counter++;
                    break;
                }
            }

            // Top-5 accuracy
            for (int x = 0; x < 5 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewer().contains(reviewers.get(x))) {
                    top5Counter++;
                    break;
                }
            }

            // Top-10 accuracy
            for (int x = 0; x < 10 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewer().contains(reviewers.get(x))) {
                    top10Counter++;
                    break;
                }
            }

            // Mean Reciprocal Rank
            for (int x = 0; x < reviewers.size(); x++) {
                if (pullRequest.getReviewer().contains(reviewers.get(x))) {
                    mrrValue += 1d / (x + 1);
                    break;
                }
            }

            printMetrics(iterationsCounter, top1Counter, top3Counter, top5Counter, top10Counter, mrrValue, pullRequest.getChangeNumber());
        }
    }

    private void printMetrics(int iterationsCounter, int top1Counter, int top3Counter, int top5Counter,
                              int top10Counter, double mrrValue, int changeRequestNumber) {
        printLine("Iterations: " + iterationsCounter);
        printLine("Change request number: " + changeRequestNumber);
        printLine("Top-1 accuracy: " + ((double) top1Counter / (double) iterationsCounter) * 100d + "%");
        printLine("Top-3 accuracy: " + ((double) top3Counter / (double) iterationsCounter) * 100d + "%");
        printLine("Top-5 accuracy: " + ((double) top5Counter / (double) iterationsCounter) * 100d + "%");
        printLine("Top-10 accuracy: " + ((double) top10Counter / (double) iterationsCounter) * 100d + "%");
        printLine("Mean Reciprocal Rank: " + mrrValue / iterationsCounter);

        printLine("top1Counter: " + top1Counter);
        printLine("top3Counter: " + top3Counter);
        printLine("top5Counter: " + top5Counter);
        printLine("top10Counter: " + top10Counter);
        printLine("mrrValue: " + mrrValue);
        printLine("iterationsCounter: " + iterationsCounter);
        printLine("_____________________________________________");
    }

    private void printLine(String text) {
        System.out.println(text);
        //logger.info(project + " " + text);
    }
}