package muni.fi.revrec.model.pullRequest;

import muni.fi.revrec.model.reviewer.Developer;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface PullRequestDAO extends CrudRepository<PullRequest, Long> {

    List<PullRequest> findByReviewerAndTimestampLessThanAndTimestampGreaterThanAndProjectName(Developer reviewer, Long timestamp1, Long timestamp2, String name);

    List<PullRequest> findByProjectNameAndTimestampLessThan(String name, Long timestamp);

    List<PullRequest> findByProjectNameOrderByTimestampDesc(String name);

    Long countByReviewerAndProjectNameAndTimestampLessThan(Developer reviewer, String project, Long timestamp);

    Long countByProjectNameAndSubProjectAndReviewerAndTimestampLessThan(String project, String subProject, Developer reviewer, Long timestamp);

    List<PullRequest> findByProjectNameAndReviewerAndTimestampLessThan(String project, Developer reviewer, Long time1);

    Long countByProjectNameAndReviewerAndOwnerAndTimestampLessThan(String project, Developer reviewer, Developer owner, Long timestamp);

    List<PullRequest> findAll();
}
