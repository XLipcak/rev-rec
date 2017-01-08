package muni.fi.reviewrecommendations.db.model.filePath;

import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface FilePathDAO extends CrudRepository<FilePath, Long> {
}
