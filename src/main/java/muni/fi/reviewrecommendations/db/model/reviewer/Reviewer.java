package muni.fi.reviewrecommendations.db.model.reviewer;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Entity
public class Reviewer {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;

    private String email;
    private String name;

    public Reviewer() {
    }

    public Reviewer(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
