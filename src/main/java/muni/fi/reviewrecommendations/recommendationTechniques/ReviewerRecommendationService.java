package muni.fi.reviewrecommendations.recommendationTechniques;

import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestDAO;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Kubo on 3.2.2017.
 */

@Service
public class ReviewerRecommendationService {

    @Autowired
    private PullRequestDAO pullRequestDAO;

    public List<Reviewer> recommend(ReviewerRecommendation reviewerRecommendation, Review review) {
        Map<Reviewer, Double> map = reviewerRecommendation.reviewersRankingAlgorithm(review);
        map = sortByValue(map);

        List<Reviewer> result = new ArrayList<>();
        for (Map.Entry<Reviewer, Double> entry : map.entrySet()) {
            System.out.println(entry.getKey().getName() + " => " + entry.getValue());
            result.add(entry.getKey());
        }

        System.out.println("Done!");

        return result;
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
}
