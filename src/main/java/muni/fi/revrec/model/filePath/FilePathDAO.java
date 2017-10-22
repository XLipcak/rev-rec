package muni.fi.revrec.model.filePath;

import muni.fi.revrec.model.reviewer.Developer;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface FilePathDAO extends CrudRepository<FilePath, Long> {
    List<FilePath> findByPullRequestProjectNameAndPullRequestReviewersAndPullRequestTimestampLessThan(String projectName,
                                                                                                     Developer reviewer, Long timestamp);

    Long countByPullRequestProjectNameAndLocationAndPullRequestReviewersAndPullRequestTimestampLessThan(String projectName, String location,
                                                                                                                Developer reviewer, Long timestamp);
}
