package muni.fi.reviewrecommendations.db.model.filePath;

import muni.fi.reviewrecommendations.db.model.reviewer.Developer;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface FilePathDAO extends CrudRepository<FilePath, Long> {
    List<FilePath> findByPullRequestProjectNameAndPullRequestReviewerAndPullRequestTimestampLessThan(String projectName,
                                                                                                     Developer reviewer, Long timestamp);

    List<FilePath> findByPullRequestProjectNameAndLocationAndPullRequestReviewerAndPullRequestTimestampLessThan(String projectName, String location,
                                                                                                                Developer reviewer, Long timestamp);
}
