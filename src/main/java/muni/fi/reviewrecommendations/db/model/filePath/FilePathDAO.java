package muni.fi.reviewrecommendations.db.model.filePath;

import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface FilePathDAO extends CrudRepository<FilePath, Long> {
    List<FilePath> findByPullRequestProjectNameAndPullRequestAllSpecificCodeReviewersAndPullRequestTimeLessThan(String projectName,
                                                                                                                Reviewer reviewer, Long time);

    List<FilePath> findByPullRequestProjectNameAndFilePathAndPullRequestAllSpecificCodeReviewersAndPullRequestTimeLessThan(String projectName, String filePath,
                                                                                                                           Reviewer reviewer, Long time);
}
