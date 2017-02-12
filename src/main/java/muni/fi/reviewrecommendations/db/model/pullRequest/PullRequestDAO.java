package muni.fi.reviewrecommendations.db.model.pullRequest;

import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface PullRequestDAO extends CrudRepository<PullRequest, Long> {

    List<PullRequest> findByTimeLessThanAndProjectName(Long time, String name);

    List<PullRequest> findByChangeIdAndProjectName(String changeId, String name);

    List<PullRequest> findByChangeNumber(Integer changeNumber);

    List<PullRequest> findByCodeReviewersAndTimeLessThanAndTimeGreaterThanAndProjectName(Reviewer reviewer, Long time1, Long time2, String name);

    List<PullRequest> findByProjectNameAndTimeLessThan(String name, Long time1);

    List<PullRequest> findByProjectName(String name);

    List<PullRequest> findByAllSpecificCodeReviewersAndProjectName(Reviewer reviewer, String project);

    Set<PullRequest> findByProjectNameAndFilePathsFilePathAndTimeLessThan(String name, String filePath, Long time1);

    List<PullRequest> findByProjectNameAndSubProjectAndAllSpecificCodeReviewersAndTimeLessThan(String project, String subProject, Reviewer reviewer, Long time1);

    List<PullRequest> findByProjectNameAndSubProjectAndTimeLessThan(String project, String subProject, Long time1);

    List<PullRequest> findByProjectNameAndAllSpecificCodeReviewersAndTimeLessThan(String project, Reviewer reviewer, Long time1);

    List<PullRequest> findByProjectNameAndAllSpecificCodeReviewersAndOwnerAndTimeLessThan(String project, Reviewer reviewer, Reviewer owner, Long time1);
}
