package muni.fi.reviewrecommendations.recommendationTechniques;

import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequest;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestDAO;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestService;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class used to recommend code reviewers in this project. It post-processes the results returned by
 * algorithms implementing ReviewerRecommendation interface and groups common functionality.
 *
 * @author Jakub Lipcak, Masaryk University
 */

@Service
public class ReviewerRecommendationService {

    @Value("${recommendation.reviewer.retired}")
    private long timeRetiredInMonths;

    @Value("${recommendation.project}")
    private String project;

    @Autowired
    private PullRequestDAO pullRequestDAO;

    @Autowired
    private PullRequestService pullRequestService;

    /**
     *
     * @param reviewerRecommendation
     * @param pullRequest
     * @return
     */
    public List<Reviewer> recommend(ReviewerRecommendation reviewerRecommendation, PullRequest pullRequest) {
        Map<Reviewer, Double> map = reviewerRecommendation.recommend(pullRequest);
        map = sortByValue(map);
        List<Reviewer> result = new ArrayList<>();
        for (Map.Entry<Reviewer, Double> entry : map.entrySet()) {
            //System.out.println(entry.getKey().getName() + " => " + entry.getValue());
            result.add(entry.getKey());
        }

        return processResult(result, pullRequest);
    }


    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
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

    private List<Reviewer> processResult(List<Reviewer> reviewList, PullRequest pullRequest) {
        List<Reviewer> result = removeRetiredReviewers(reviewList, pullRequest);

        for (int x = 0; x < result.size(); x++) {
            System.out.println((x + 1) + " " + result.get(x).getName());
        }
        return result;
    }

    private List<Reviewer> removeRetiredReviewers(List<Reviewer> reviewersList, PullRequest pullRequest) {
        long timeRetired = timeRetiredInMonths * 30 * 24 * 60 * 60 * 1000;
        List<Reviewer> result = new ArrayList<>();
        List<Reviewer> removedReviewers = new ArrayList<>();
        for (Reviewer reviewer : reviewersList) {
            if (pullRequestService.findByCodeReviewersAndTimeLessThanAndTimeGreaterThanAndProjectName(reviewer, pullRequest.getTime(), pullRequest.getTime() - timeRetired, project).size() > 0) {
                result.add(reviewer);
            } else {
                removedReviewers.add(reviewer);
            }
        }
        for (int x = 0; x < removedReviewers.size(); x++) {
            result.add(removedReviewers.get(x));
        }
        return result;
    }

}
