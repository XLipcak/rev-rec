package muni.fi.reviewrecommendations.recommendationTechniques;

import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;

import java.util.Map;

/**
 * @author Jakub Lipcak, Masaryk University
 */
public interface ReviewerRecommendation {
    /**
     * Reviewer recommendation algorithm.
     *
     * @param review Review, for which we want to find appropriate code reviewer.
     * @return Sorted map of reviewers with points assigned to each of them.
     */
    Map<Reviewer, Double> reviewersRankingAlgorithm(Review review);
}
