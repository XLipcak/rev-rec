package muni.fi.reviewrecommendations.db.model.pullRequest;

import muni.fi.reviewrecommendations.db.model.reviewer.Developer;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface PullRequestDAO extends CrudRepository<PullRequest, Long> {

    List<PullRequest> findByTimestampLessThanAndProjectName(Long timestamp, String name);

    List<PullRequest> findByChangeIdAndProjectName(String changeId, String name);

    List<PullRequest> findByChangeNumber(Integer changeNumber);

    List<PullRequest> findByReviewerAndTimestampLessThanAndTimestampGreaterThanAndProjectName(Developer reviewer, Long timestamp1, Long timestamp2, String name);

    List<PullRequest> findByProjectNameAndTimestampLessThan(String name, Long timestamp);

    List<PullRequest> findByProjectName(String name);

    List<PullRequest> findByProjectNameOrderByTimestampDesc(String name);

    List<PullRequest> findByReviewerAndProjectNameAndTimestampLessThan(Developer reviewer, String project, Long timestamp);

    List<PullRequest> findByProjectNameAndSubProjectAndReviewerAndTimestampLessThan(String project, String subProject, Developer reviewer, Long timestamp);

    List<PullRequest> findByProjectNameAndSubProjectAndTimestampLessThan(String project, String subProject, Long timestamp);

    List<PullRequest> findByProjectNameAndReviewerAndTimestampLessThan(String project, Developer reviewer, Long time1);

    List<PullRequest> findByProjectNameAndReviewerAndOwnerAndTimestampLessThan(String project, Developer reviewer, Developer owner, Long timestamp);
}
