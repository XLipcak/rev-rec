package muni.fi.reviewrecommendations.db.model.pullRequest;

import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface PullRequestDAO extends CrudRepository<PullRequest, Long> {
    //findByTimeLessThan
    List<PullRequest> findByTimeLessThan(Long time);
    List<PullRequest> findByTimeLessThanAndTimeGreaterThan(Long time1, Long time2);
    List<PullRequest> findByChangeId(String changeId);
    List<PullRequest> findByChangeNumber(Integer changeNumber);
}
