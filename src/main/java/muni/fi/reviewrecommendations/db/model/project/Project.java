package muni.fi.reviewrecommendations.db.model.project;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Entity
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Integer id;

    private String projectName;

    public Project() {
    }

    public Project(String projectName) {
        this.projectName = projectName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
