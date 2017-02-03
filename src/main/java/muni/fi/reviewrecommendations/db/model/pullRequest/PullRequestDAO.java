package muni.fi.reviewrecommendations.db.model.pullRequest;

import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface PullRequestDAO extends CrudRepository<PullRequest, Long> {

    List<PullRequest> findByTimeLessThanAndProjectName(Long time, String name);
    List<PullRequest> findByChangeIdAndProjectName(String changeId, String name);
    List<PullRequest> findByChangeNumber(Integer changeNumber);
    List<PullRequest> findByCodeReviewersAndTimeLessThanAndTimeGreaterThanAndProjectName(Reviewer reviewer, Long time1, Long time2, String name);
    List<PullRequest> findByProjectName(String name);
}
