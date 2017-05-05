package muni.fi.revrec.model.reviewer;

import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface ReviewerDAO extends CrudRepository<Developer, Integer> {
    Developer findByEmail(String email);
}
