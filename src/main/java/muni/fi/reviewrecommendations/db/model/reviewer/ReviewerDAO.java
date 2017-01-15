package muni.fi.reviewrecommendations.db.model.reviewer;

import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Transactional
public interface ReviewerDAO extends CrudRepository<Reviewer, Integer> {
    Reviewer findByEmail(String email);
}
