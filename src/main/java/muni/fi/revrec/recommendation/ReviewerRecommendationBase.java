package muni.fi.revrec.recommendation;

import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.pullRequest.PullRequestDAO;
import muni.fi.revrec.model.reviewer.Developer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base abstract class containing methods, which are useful for all Code Reviewers Recommendation Algorithms.
 *
 * @author Jakub Lipcak, Masaryk University
 */
public abstract class ReviewerRecommendationBase {

    protected PullRequestDAO pullRequestDAO;
    protected String project;
    private boolean removeRetiredReviewers;
    private long timeRetiredInMonths;


    public ReviewerRecommendationBase(PullRequestDAO pullRequestDAO, boolean removeRetiredReviewers, long timeRetiredInMonths, String project) {
        this.pullRequestDAO = pullRequestDAO;
        this.removeRetiredReviewers = removeRetiredReviewers;
        this.timeRetiredInMonths = timeRetiredInMonths;
        this.project = project;
    }

    protected List<Developer> processResult(Map<Developer, Double> map, PullRequest pullRequest) {

        //sort reviewers into the list
        map = sortByValue(map);
        List<Developer> result = new ArrayList<>();
        for (Map.Entry<Developer, Double> entry : map.entrySet()) {
            result.add(entry.getKey());
        }

        //remove retired reviewers
        if (removeRetiredReviewers)
            result = removeRetiredReviewers(result, pullRequest);

        //print recommended reviewers
        for (int x = 0; x < result.size(); x++) {
            System.out.println((x + 1) + " " + result.get(x).getName());
        }

        return result;
    }


    protected <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
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
        result.addAll(removedReviewers);
        return result;
    }

}
