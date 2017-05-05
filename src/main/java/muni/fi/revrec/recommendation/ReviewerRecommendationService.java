package muni.fi.revrec.recommendation;

import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.pullRequest.PullRequestDAO;
import muni.fi.revrec.model.reviewer.Developer;
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

    /**
     * @param reviewerRecommendation
     * @param pullRequest
     * @return
     */
    public List<Developer> recommend(ReviewerRecommendation reviewerRecommendation, PullRequest pullRequest) {
        Map<Developer, Double> map = reviewerRecommendation.recommend(pullRequest);
        map = sortByValue(map);
        List<Developer> result = new ArrayList<>();
        for (Map.Entry<Developer, Double> entry : map.entrySet()) {
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

    private List<Developer> processResult(List<Developer> reviewList, PullRequest pullRequest) {
        List<Developer> result = removeRetiredReviewers(reviewList, pullRequest);

        for (int x = 0; x < result.size(); x++) {
            System.out.println((x + 1) + " " + result.get(x).getName());
        }
        return result;
    }

    private List<Developer> removeRetiredReviewers(List<Developer> reviewersList, PullRequest pullRequest) {
        long timeRetired = timeRetiredInMonths * 30 * 24 * 60 * 60 * 1000;
        List<Developer> result = new ArrayList<>();
        List<Developer> removedReviewers = new ArrayList<>();
        for (Developer reviewer : reviewersList) {
            if (pullRequestDAO.findByReviewerAndTimestampLessThanAndTimestampGreaterThanAndProjectName(reviewer, pullRequest.getTimestamp(), pullRequest.getTimestamp() - timeRetired, project).size() > 0) {
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
