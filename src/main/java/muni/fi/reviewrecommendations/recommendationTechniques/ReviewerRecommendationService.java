package muni.fi.reviewrecommendations.recommendationTechniques;

import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestDAO;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestService;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ReviewerRecommendationService
 *
 * @author Jakub Lipcak, Masaryk University
 */

@Service
public class ReviewerRecommendationService {

    @Value("${recommendation.reviewer.retired}")
    private int timeRetiredInMonths;

    @Value("${recommendation.project}")
    private String project;

    @Autowired
    private PullRequestDAO pullRequestDAO;

    @Autowired
    private PullRequestService pullRequestService;

    public List<Reviewer> recommend(ReviewerRecommendation reviewerRecommendation, Review review) {
        Map<Reviewer, Double> map = reviewerRecommendation.reviewersRankingAlgorithm(review);
        map = sortByValue(map);
        List<Reviewer> result = new ArrayList<>();
        for (Map.Entry<Reviewer, Double> entry : map.entrySet()) {
            //System.out.println(entry.getKey().getName() + " => " + entry.getValue());
            result.add(entry.getKey());
        }

        return processResult(result, review);
    }

    public List<Reviewer> recommend(ReviewerRecommendation reviewerRecommendation1, ReviewerRecommendation reviewerRecommendation2, Review review) {
        Map<Reviewer, Double> map1 = reviewerRecommendation1.reviewersRankingAlgorithm(review);
        map1 = sortByValue(map1);
        List<Reviewer> result1 = new ArrayList<>();
        for (Map.Entry<Reviewer, Double> entry : map1.entrySet()) {
            result1.add(entry.getKey());
        }

        Map<Reviewer, Double> map2 = reviewerRecommendation2.reviewersRankingAlgorithm(review);
        map2 = sortByValue(map2);
        List<Reviewer> result2 = new ArrayList<>();
        for (Map.Entry<Reviewer, Double> entry : map2.entrySet()) {
            result2.add(entry.getKey());
        }

        List<Reviewer> result = combine(result1, result2);

        return processResult(result, review);
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

    private List<Reviewer> processResult(List<Reviewer> reviewList, Review review) {
        List<Reviewer> result;  //= removeRetiredReviewers(reviewList, review); TODO: uncomment later
        if (reviewList.size() > 10) {
            result = reviewList.subList(0, 10);
        } else {
            result = reviewList;
        }
        for (int x = 0; x < result.size(); x++) {
            System.out.println((x + 1) + " " + result.get(x).getName());
        }
        return result;
    }

    private List<Reviewer> removeRetiredReviewers(List<Reviewer> reviewersList, Review review) {
        long timeRetired = timeRetiredInMonths * 30 * 24 * 60 * 60 * 1000;
        List<Reviewer> result = new ArrayList<>();
        for (Reviewer reviewer : reviewersList) {
            if (pullRequestService.findByCodeReviewersAndTimeLessThanAndTimeGreaterThanAndProjectName(reviewer, review.getTime(), review.getTime() - timeRetired, project).size() > 0) {
                result.add(reviewer);
            }
        }
        return result;
    }

    private List<Reviewer> combine(List<Reviewer> list1, List<Reviewer> list2) {
        Map<Reviewer, Double> resultMap = new HashMap<>();
        for (int x = 0; x < list1.size(); x++) {
            Reviewer reviewer = list1.get(x);
            if (resultMap.containsKey(reviewer)) {
                resultMap.replace(reviewer, resultMap.get(reviewer) + x);
            } else {
                resultMap.put(reviewer, (double) x);
            }
        }

        for (int x = 0; x < list2.size(); x++) {
            Reviewer reviewer = list2.get(x);
            if (resultMap.containsKey(reviewer)) {
                resultMap.replace(reviewer, resultMap.get(reviewer) + x + (x / 100d));
            } else {
                resultMap.put(reviewer, (double) x + (x / 100d));
            }
        }

        resultMap = sortByValue(resultMap);
        List<Reviewer> reverseResult = new ArrayList<>();
        for (Map.Entry<Reviewer, Double> entry : resultMap.entrySet()) {
            reverseResult.add(entry.getKey());
        }
        List<Reviewer> result = new ArrayList<>();
        for (int x = reverseResult.size() - 1; x >= 0; x--) {
            result.add(reverseResult.get(x));
        }

        return result;
    }
}
