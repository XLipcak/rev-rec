package muni.fi.revrec.model.project;

import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface ProjectDAO extends CrudRepository<Project, String> {
}
