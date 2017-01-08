package muni.fi.reviewrecommendations.db.model.project;

import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface ProjectDAO extends CrudRepository<Project, Long> {
}
