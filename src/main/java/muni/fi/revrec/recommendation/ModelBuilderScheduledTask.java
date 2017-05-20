package muni.fi.revrec.recommendation;

import muni.fi.revrec.recommendation.bayesrec.BayesRec;
import muni.fi.revrec.recommendation.revfinder.RevFinder;
import muni.fi.revrec.recommendation.reviewbot.ReviewBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ModelBuilderScheduledTask class is used to schedule
 * tasks related to models of reviewers recommendation systems.
 *
 * @author Jakub Lipcak, Masaryk University
 */
@Component
public class ModelBuilderScheduledTask {

    @Autowired
    private RevFinder revFinder;

    @Autowired
    private ReviewBot reviewBot;

    @Autowired
    private BayesRec bayesRec;

    /**
     * Build models of reviewers recommendation systems.
     */
    //@Scheduled(cron = "${recommendation.jobs.buildModel.cron}") //uncomment this line, if you want to schedule this task
    public void buildModels() {
        revFinder.buildModel();
        reviewBot.buildModel();
        bayesRec.buildModel();
    }
}
