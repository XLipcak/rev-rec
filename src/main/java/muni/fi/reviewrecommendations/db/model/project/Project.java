package muni.fi.reviewrecommendations.db.model.project;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Entity
public class Project {
    @Id
    private String name;

    public Project() {
    }

    public Project(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String projectName) {
        this.name = projectName;
    }
}
