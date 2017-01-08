package muni.fi.reviewrecommendations;

import com.google.common.collect.Lists;
import muni.fi.reviewrecommendations.common.GerritBrowser;
import muni.fi.reviewrecommendations.common.GitBrowser;
import muni.fi.reviewrecommendations.db.DataLoader;
import muni.fi.reviewrecommendations.db.model.filePath.FilePath;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequest;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestDAO;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import muni.fi.reviewrecommendations.recommendationTechniques.ReviewerRecommendation;
import muni.fi.reviewrecommendations.recommendationTechniques.revfinder.RevFinder;
import muni.fi.reviewrecommendations.recommendationTechniques.revfinder.Review;
import muni.fi.reviewrecommendations.recommendationTechniques.reviewbot.ReviewBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Kubo on 8.1.2017.
 */
@Component
public class InitialLoader implements CommandLineRunner {


    @Autowired
    private DataLoader dataLoader;

    @Autowired
    private PullRequestDAO pullRequestDAO;

    @Override
    public void run(String... strings) throws Exception {
        GerritBrowser gerritBrowser = new GerritBrowser("https://android-review.googlesource.com");
        GitBrowser gitBrowser = new GitBrowser("sdk", false);

        String changeId = "I212329c925370b6cd01db1b7260e7ba5f083cea0";

        Review review = new Review();
        review.setFilePaths(gerritBrowser.getFilePaths(changeId));

        RevFinder revFinder = new RevFinder(getAllPreviousReviews("I212329c925370b6cd01db1b7260e7ba5f083cea0"));
        ReviewBot reviewBot = new ReviewBot(gitBrowser, gerritBrowser);

        recommend(reviewBot, review);
    }


    private static void recommend(ReviewerRecommendation reviewerRecommendation, Review review) {
        Map<Reviewer, Double> map = reviewerRecommendation.reviewersRankingAlgorithm(review);
        map = sortByValue(map);
        for (Map.Entry<Reviewer, Double> entry : map.entrySet()) {
            System.out.println(entry.getKey().getName() + " => " + entry.getValue());
        }

        System.out.println("Done!");
    }

    private List<Review> getAllPreviousReviews(String changeId) {

        List<Review> allPreviousReviews = new ArrayList<>();

        PullRequest request = pullRequestDAO.findByChangeId(changeId).get(0);
        Set<PullRequest> pullRequests = new HashSet<>(Lists.newArrayList(pullRequestDAO.findByTimeLessThan(request.getTime())));

        for (PullRequest pullRequest : pullRequests) {
            Review review = new Review();
            review.setDate(new Date(pullRequest.getTime() * 1000));

            List<String> filePaths = new ArrayList<>();
            for (FilePath filePath : pullRequest.getFilePaths()) {
                filePaths.add(filePath.getFilePath());
            }
            review.setFilePaths(filePaths);
            review.setReviewers(new ArrayList<>(pullRequest.getReviewers()));

            allPreviousReviews.add(review);
        }
        return allPreviousReviews;
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
}
