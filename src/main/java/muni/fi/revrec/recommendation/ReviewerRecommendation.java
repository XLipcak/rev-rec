package muni.fi.revrec.recommendation;

import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.reviewer.Developer;

import java.util.List;

/**
 * ReviewerRecommendation interface prescribes methods, which should be implemented by code reviewer recommendation algorithms.
 *
 * @author Jakub Lipcak, Masaryk University
 */
public interface ReviewerRecommendation {

    /**
     * Build model for the reviewer recommendation algorithm.
     */
    void buildModel();

    /**
     * Code reviewer recommendation algorithm. It returns the map of reviewers with scores recommended to review given PullRequest.
     *
     * @param pullRequest PullRequest, for which we want to find appropriate code reviewers.
     * @return Sorted map of reviewers with points assigned to each of them by reviewer recommendation algorithms.
     */
    List<Developer> recommend(PullRequest pullRequest);

}
