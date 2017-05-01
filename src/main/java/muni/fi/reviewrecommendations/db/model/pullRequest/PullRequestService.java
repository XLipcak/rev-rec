package muni.fi.reviewrecommendations.db.model.pullRequest;

import muni.fi.reviewrecommendations.db.model.reviewer.Developer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Kubo on 3.2.2017.
 */
@Service
public class PullRequestService {

    @Autowired
    private PullRequestDAO pullRequestDAO;

    public List<PullRequest> findByTimeLessThanAndProjectName(Long time, String name) {
        return pullRequestDAO.findByTimestampLessThanAndProjectName(time, name);
    }

    public List<PullRequest> findByProjectNameOrderByTimeDesc(String projectName) {
        return pullRequestDAO.findByProjectNameOrderByTimestampDesc(projectName);
    }

    public List<PullRequest> findByChangeIdAndProjectName(String changeId, String name) {
        return pullRequestDAO.findByChangeIdAndProjectName(changeId, name);
    }

    public List<PullRequest> findByChangeNumber(Integer changeNumber) {
        return pullRequestDAO.findByChangeNumber(changeNumber);
    }

    public List<PullRequest> findByCodeReviewersAndTimeLessThanAndTimeGreaterThanAndProjectName(Developer reviewer, Long time1, Long time2, String name) {
        return pullRequestDAO.findByReviewerAndTimestampLessThanAndTimestampGreaterThanAndProjectName(reviewer, time1, time2, name);
    }

    public List<PullRequest> findByProjectName(String name) {
        return pullRequestDAO.findByProjectName(name);
    }

    public List<PullRequest> getAllPreviousReviews(Long changeTime, String projectName) {
        return pullRequestDAO.findByTimestampLessThanAndProjectName(changeTime, projectName);
    }

    private String removeSlash(String filePath) {
        String result = "";

        for (int x = 0; x < filePath.length(); x++) {
            if (filePath.charAt(x) != '/') {
                result += filePath.charAt(x);
            }
        }
        return result;
    }
}
