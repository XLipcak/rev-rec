package muni.fi.revrec.recommendation;

import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.reviewer.Developer;

import java.util.List;

/**
 * ReviewerRecommendation interface prescribes methods, which should be implemented by Code Reviewers Recommendation Algorithms.
 *
 * @author Jakub Lipcak, Masaryk University
 */
public interface ReviewerRecommendation {

    /**
     * Build model necessary to recommend reviewers by the algorithm.
     */
    void buildModel();

    /**
     * Code Reviewers Recommendation Algorithm. It returns the list of recommended code reviewers sorted by their relevance.
     * The most relevant candidate is at the index 0.
     *
     * @param pullRequest PullRequest, for which we want to find appropriate code reviewers.
     * @return Sorted list of recommended code reviewers.
     */
    List<Developer> recommend(PullRequest pullRequest);

}
