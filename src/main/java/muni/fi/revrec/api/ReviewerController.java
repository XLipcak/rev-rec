package muni.fi.revrec.api;

import muni.fi.revrec.common.GerritService;
import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.recommendation.bayesrec.BayesRec;
import muni.fi.revrec.recommendation.revfinder.RevFinder;
import muni.fi.revrec.recommendation.reviewbot.ReviewBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST interface of the system.
 *
 * @author Jakub Lipcak, Masaryk University
 */
@RestController
@RequestMapping(path = "/api")
public class ReviewerController {

    private enum RecommendationMethod {
        REVIEWBOT, REVFINDER, BAYES
    }

    @Autowired
    private RevFinder revFinder;

    @Autowired
    private ReviewBot reviewBot;

    @Autowired
    private BayesRec bayesRec;

    @Autowired
    private GerritService gerritService;

    @RequestMapping(value = "/reviewers-recommendation", method = RequestMethod.GET)
    ResponseEntity<?> recommend(@RequestParam(value = "gerritChangeNumber", required = true) String gerritChangeNumber,
                                @RequestParam(value = "recommendationMethod", required = false) RecommendationMethod recommendationMethod) {
        PullRequest pullRequest = gerritService.getPullRequest(gerritChangeNumber);

        if (recommendationMethod != null) {
            switch (recommendationMethod) {
                case REVIEWBOT:
                    return ResponseEntity.ok(reviewBot.recommend(pullRequest));
                case REVFINDER:
                    return ResponseEntity.ok(revFinder.recommend(pullRequest));
                case BAYES:
                    return ResponseEntity.ok(bayesRec.recommend(pullRequest));
            }
        }

        return ResponseEntity.ok(revFinder.recommend(pullRequest));
    }
}
