package muni.fi.reviewrecommendations.recommendationTechniques;

import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;

import java.util.Map;

/**
 * @author Jakub Lipcak, Masaryk University
 */
public interface ReviewerRecommendation {
    Map<Reviewer, Double> reviewersRankingAlgorithm(Review review);
}
