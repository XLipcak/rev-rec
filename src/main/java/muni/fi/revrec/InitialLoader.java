package muni.fi.revrec;

import muni.fi.revrec.common.GerritService;
import muni.fi.revrec.common.MailService;
import muni.fi.revrec.common.data.DataLoader;
import muni.fi.revrec.model.filePath.FilePathDAO;
import muni.fi.revrec.model.project.Project;
import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.pullRequest.PullRequestDAO;
import muni.fi.revrec.model.reviewer.Developer;
import muni.fi.revrec.recommendation.bayesrec.BayesRec;
import muni.fi.revrec.recommendation.revfinder.RevFinder;
import muni.fi.revrec.recommendation.reviewbot.ReviewBot;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
    private FilePathDAO filePathDAO;

    @Autowired
    private GerritService gerritService;

    @Autowired
    private BayesRec bayesRec;

    @Autowired
    private RevFinder revFinder;

    @Autowired
    private ReviewBot reviewBot;

    @Autowired
    private MailService mailService;

    @Autowired
    DataLoader dataLoader;

    String output = "";

    @Override
    public void run(String... strings) throws Exception {
        String[] mailAddresses = {"jakublipcak@gmail.com"};

        List<Project> projects = new ArrayList<>();

        /*
            Finished projects
         */
//        projects.add(new Project("android", "https://android-review.googlesource.com/"));
//        projects.add(new Project("angular.js", "https://github.com/angular/angular.js"));
//        projects.add(new Project("angular", "https://api.github.com/repos/angular/angular"));
//        projects.add(new Project("chromium", "https://chromium-review.googlesource.com"));
//        projects.add(new Project("eclipse", "https://git.eclipse.org/r/"));
//        projects.add(new Project("gem5", "https://gem5-review.googlesource.com"));
//        projects.add(new Project("go", "https://go-review.googlesource.com"));
//        projects.add(new Project("gwt", "https://gwt-review.googlesource.com"));
//        projects.add(new Project("jquery", "https://github.com/jquery/jquery"));
//        projects.add(new Project("kitware", "http://review.source.kitware.com/"));
//        projects.add(new Project("lineageos", "https://review.lineageos.org/"));
//        projects.add(new Project("openstack", "https://review.openstack.org/"));
//        projects.add(new Project("qt", "https://codereview.qt-project.org"));
//        projects.add(new Project("react", "https://github.com/facebook/react"));
//        projects.add(new Project("react-native", "https://github.com/facebook/react-native"));
//        projects.add(new Project("typo3", "https://review.typo3.org/"));
//        projects.add(new Project("revealjs", "https://api.github.com/repos/hakimel/reveal.js"));
//        projects.add(new Project("vue", "https://api.github.com/repos/vuejs/vue"));
//        projects.add(new Project("ionic", "https://api.github.com/repos/ionic-team/ionic"));
//        projects.add(new Project("requests", "https://api.github.com/repos/requests/requests"));
//        projects.add(new Project("webpack", "https://api.github.com/repos/webpack/webpack"));
//        projects.add(new Project("redux", "https://api.github.com/repos/reactjs/redux"));
//        projects.add(new Project("oh-my-zsh", "https://api.github.com/repos/robbyrussell/oh-my-zsh"));


        /*
            To be tested projects
         */

        projects.add(new Project("atom", "https://api.github.com/repos/atom/atom"));
        projects.add(new Project("django", "https://api.github.com/repos/django/django"));
        projects.add(new Project("jekyll", "https://api.github.com/repos/jekyll/jekyll"));
        projects.add(new Project("laravel", "https://api.github.com/repos/laravel/laravel"));
        projects.add(new Project("material-ui", "https://api.github.com/repos/callemall/material-ui"));
        projects.add(new Project("meteor", "https://api.github.com/repos/meteor/meteor"));
        projects.add(new Project("moment", "https://api.github.com/repos/moment/moment"));
        projects.add(new Project("swift", "https://api.github.com/repos/apple/swift"));
        projects.add(new Project("tensorflow", "https://api.github.com/repos/tensorflow/tensorflow"));
        projects.add(new Project("threejs", "https://api.github.com/repos/mrdoob/three.js"));
        projects.add(new Project("RxJava", "https://api.github.com/repos/ReactiveX/RxJava"));
        projects.add(new Project("scilab", "https://codereview.scilab.org/"));
        projects.add(new Project("libreoffice", "https://gerrit.libreoffice.org/"));
        projects.add(new Project("gerrit", "https://gerrit-review.googlesource.com"));


        /*
            To be mined projects
         */

        //        projects.add(new Project("homebrew-core", "https://api.github.com/repos/Homebrew/homebrew-core"));
        //        projects.add(new Project("yarn", "https://api.github.com/repos/yarnpkg/yarn"));
        //        projects.add(new Project("brackets", "https://api.github.com/repos/adobe/brackets"));
        //        projects.add(new Project("kubernetes", "https://api.github.com/repos/kubernetes/kubernetes"));

        //        projects.add(new Project("bootstrap", "https://api.github.com/repos/twbs/bootstrap"));
        //        projects.add(new Project("spring-boot", "https://api.github.com/repos/spring-projects/spring-boot"));
        //        projects.add(new Project("opencv", "https://api.github.com/repos/opencv/opencv"));
        //        projects.add(new Project("spark", "https://api.github.com/repos/apache/spark"));
        //        projects.add(new Project("moby", "https://api.github.com/repos/moby/moby"));
        //        projects.add(new Project("cgm-remote-monitor", "https://api.github.com/repos/nightscout/cgm-remote-monitor"));
        //        projects.add(new Project("scikit-learn", "https://api.github.com/repos/scikit-learn/scikit-learn"));
        //        projects.add(new Project("spring-framework", "https://api.github.com/repos/spring-projects/spring-framework"));
        //        projects.add(new Project("bitcoin", "https://api.github.com/repos/bitcoin/bitcoin"));
        //        projects.add(new Project("redis", "https://api.github.com/repos/antirez/redis"));


        for (Project project : projects) {
            try {
                printLine("_____________________________________________");
                dataLoader.initDbFromJson(project.getName(), project.getProjectUrl());
                printLine("Analysis of project: " + project.getName());
                printLine("Repository: " + project.getProjectUrl());
                printLine("Pull requests: " + pullRequestDAO.findAll().size());

                // 1.) Revfinder
                printLine("Evaluating REVFINDER");
                revFinder = new RevFinder(pullRequestDAO, false, 12, project.getName(), false);
                evaluateRevFinderAlgorithm(project.getName());

                // 2.) Revfinder+
                printLine("Evaluating REVFINDER+");
                revFinder = new RevFinder(pullRequestDAO, true, 12, project.getName(), true);
                evaluateRevFinderAlgorithm(project.getName());

                // 3.) NB
                printLine("Evaluating NB");
                bayesRec = new BayesRec(pullRequestDAO, filePathDAO, true, 12, project.getName(), false);
                evaluateBayesRecAlgorithm(project.getName());

                // 4.) NB+
                printLine("Evaluating NB+");
                bayesRec = new BayesRec(pullRequestDAO, filePathDAO, true, 12, project.getName(), true);
                evaluateBayesRecAlgorithm(project.getName());

            } catch (Exception ex) {
                try {
                    for (String address : mailAddresses) {
                        mailService.sendSimpleMessage(address, "Error", output + "\n" + ex.getMessage());
                    }
                } catch (Exception mailEx) {
                    printLine("Emails could not be sent: " + mailEx.getMessage());
                }
                continue;
            }

            try {
                for (String address : mailAddresses) {
                    mailService.sendSimpleMessage(address, "Results for project [" + project.getName() + "]", output);
                }
            } catch (Exception mailEx) {
                printLine("Emails could not be sent: " + mailEx.getMessage());
            }
            output = "";
        }


//        for (Project x : projects) {
//            System.out.println("Processing project: " + x.getName());
//            dataLoader.deleteData();
//            try {
//                dataLoader.fetchData(x.getName(), x.getProjectUrl());
//            } catch (Exception ex) {
//                logger.error(ex);
//                dataLoader.saveDataToFile(x.getName());
//            }
//        }
    }

    /**
     * Evaluate the RevFinder algorithm using the metrics Top-k Accuracy and Mean Reciprocal Rank.
     */
    private void evaluateRevFinderAlgorithm(String projectName) {

        // init variables
        int top1Counter = 0;
        int top3Counter = 0;
        int top5Counter = 0;
        int top10Counter = 0;
        double mrrValue = 0;
        int iterationsCounter = 0;
        List<PullRequest> pullRequests = pullRequestDAO.findByProjectNameOrderByTimestampDesc(projectName);

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
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top1Counter++;
                    break;
                }
            }

            // Top-3 accuracy
            for (int x = 0; x < 3 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top3Counter++;
                    break;
                }
            }

            // Top-5 accuracy
            for (int x = 0; x < 5 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top5Counter++;
                    break;
                }
            }

            // Top-10 accuracy
            for (int x = 0; x < 10 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top10Counter++;
                    break;
                }
            }

            // Mean Reciprocal Rank
            for (int x = 0; x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    mrrValue += 1d / (x + 1);
                    break;
                }
            }

            if (iterationsCounter % 100 == 0) {
                System.out.println("Processed: " + iterationsCounter + " / " + pullRequests.size());
            }
        }
        printMetrics(iterationsCounter, top1Counter, top3Counter, top5Counter, top10Counter, mrrValue);
    }

    /**
     * Evaluate the Naive Bayes-based recommendation algorithm using the metrics Top-k Accuracy and Mean Reciprocal Rank.
     */
    private void evaluateBayesRecAlgorithm(String projectName) {

        // init variables
        int top1Counter = 0;
        int top3Counter = 0;
        int top5Counter = 0;
        int top10Counter = 0;
        double mrrValue = 0;
        int iterationsCounter = 0;
        List<PullRequest> pullRequests = pullRequestDAO.findByProjectNameOrderByTimestampDesc(projectName);
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
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top1Counter++;
                    break;
                }
            }

            // Top-3 accuracy
            for (int x = 0; x < 3 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top3Counter++;
                    break;
                }
            }

            // Top-5 accuracy
            for (int x = 0; x < 5 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top5Counter++;
                    break;
                }
            }

            // Top-10 accuracy
            for (int x = 0; x < 10 && x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    top10Counter++;
                    break;
                }
            }

            // Mean Reciprocal Rank
            for (int x = 0; x < reviewers.size(); x++) {
                if (pullRequest.getReviewers().contains(reviewers.get(x))) {
                    mrrValue += 1d / (x + 1);
                    break;
                }
            }

            if (iterationsCounter % 100 == 0) {
                System.out.println("Processed: " + iterationsCounter + " / " + pullRequests.size());
            }
        }
        printMetrics(iterationsCounter, top1Counter, top3Counter, top5Counter, top10Counter, mrrValue);
    }

    private void printMetrics(int iterationsCounter, int top1Counter, int top3Counter, int top5Counter,
                              int top10Counter, double mrrValue) {
        printLine("Iterations: " + iterationsCounter);
        printLine("Top-1 accuracy: " + ((double) top1Counter / (double) iterationsCounter) * 100d + "%");
        printLine("Top-3 accuracy: " + ((double) top3Counter / (double) iterationsCounter) * 100d + "%");
        printLine("Top-5 accuracy: " + ((double) top5Counter / (double) iterationsCounter) * 100d + "%");
        printLine("Top-10 accuracy: " + ((double) top10Counter / (double) iterationsCounter) * 100d + "%");
        printLine("Mean Reciprocal Rank: " + mrrValue / iterationsCounter);

//        printLine("top1Counter: " + top1Counter);
//        printLine("top3Counter: " + top3Counter);
//        printLine("top5Counter: " + top5Counter);
//        printLine("top10Counter: " + top10Counter);
//        printLine("mrrValue: " + mrrValue);
//        printLine("iterationsCounter: " + iterationsCounter);
        printLine("_____________________________________________");
    }

    private void printLine(String text) {
        System.out.println(text);
        output += "\n" + text;
        //logger.info(project + " " + text);
    }
}