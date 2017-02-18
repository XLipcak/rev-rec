package muni.fi.reviewrecommendations.db.model.pullRequest;

import com.google.common.collect.Lists;
import muni.fi.reviewrecommendations.db.model.filePath.FilePath;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import muni.fi.reviewrecommendations.recommendationTechniques.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by Kubo on 3.2.2017.
 */
@Service
public class PullRequestService {

    @Autowired
    private PullRequestDAO pullRequestDAO;

    public List<PullRequest> findByTimeLessThanAndProjectName(Long time, String name) {
        return pullRequestDAO.findByTimeLessThanAndProjectName(time, name);
    }

    public List<PullRequest> findByProjectNameOrderByTimeDesc(String projectName) {
        return pullRequestDAO.findByProjectNameOrderByTimeDesc(projectName);
    }

    public List<PullRequest> findByChangeIdAndProjectName(String changeId, String name) {
        return pullRequestDAO.findByChangeIdAndProjectName(changeId, name);
    }

    public List<PullRequest> findByChangeNumber(Integer changeNumber) {
        return pullRequestDAO.findByChangeNumber(changeNumber);
    }

    public List<PullRequest> findByCodeReviewersAndTimeLessThanAndTimeGreaterThanAndProjectName(Reviewer reviewer, Long time1, Long time2, String name) {
        return pullRequestDAO.findByCodeReviewersAndTimeLessThanAndTimeGreaterThanAndProjectName(reviewer, time1, time2, name);
    }

    public List<PullRequest> findByProjectName(String name) {
        return pullRequestDAO.findByProjectName(name);
    }

    public List<Review> getAllPreviousReviews(Long changeTime, String projectName) {

        List<Review> allPreviousReviews = new ArrayList<>();
        //Set<Reviewer> reviewersWithAtLeastOneReview = new HashSet<>();

        Set<PullRequest> pullRequests = new HashSet<>(Lists.newArrayList(pullRequestDAO.findByTimeLessThanAndProjectName(changeTime, projectName)));

        /*for (PullRequest pullRequest : pullRequests) {
            for (Reviewer reviewer : pullRequest.getCodeReviewers()) {
                reviewersWithAtLeastOneReview.add(reviewer);
            }
        }*/

        for (PullRequest pullRequest : pullRequests) {
            Review review = new Review();
            review.setTime(pullRequest.getTime() * 1000);

            List<String> filePaths = new ArrayList<>();
            for (FilePath filePath : pullRequest.getFilePaths()) {
                //filePaths.add(pullRequest.getSubProject().hashCode() + "/" + filePath.getFilePath());     //Improvement in data for the algorithm
                filePaths.add(filePath.getFilePath());
            }
            review.setFilePaths(filePaths);
            review.setTime(pullRequest.getTime());
            review.setInsertions(pullRequest.getInsertions());
            review.setDeletions(pullRequest.getDeletions());
            review.setSubProject(pullRequest.getSubProject());
            review.setOwner(pullRequest.getOwner());

            review.setReviewers(new ArrayList<>(pullRequest.getAllSpecificCodeReviewers()));
            //review.setReviewers(new ArrayList<>(mergeReviewers(pullRequest, reviewersWithAtLeastOneReview)));

            allPreviousReviews.add(review);
        }
        return allPreviousReviews;
    }
}
