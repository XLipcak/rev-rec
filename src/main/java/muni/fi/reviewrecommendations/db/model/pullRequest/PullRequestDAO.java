package muni.fi.reviewrecommendations.db.model.pullRequest;

import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface PullRequestDAO extends CrudRepository<PullRequest, Long> {
    List<PullRequest> findByTimeLessThan(Long time);
    List<PullRequest> findByChangeId(String changeId);
    List<PullRequest> findByChangeNumber(Integer changeNumber);
}
