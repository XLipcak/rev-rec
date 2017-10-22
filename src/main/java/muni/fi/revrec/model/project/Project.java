package muni.fi.revrec.model.project;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Entity
public class Project {
    @Id
    private String name;

    @Column(name = "project_url")
    private String projectUrl;

    public Project() {
    }

    public Project(String name, String projectUrl) {
        this.name = name;
        this.projectUrl = projectUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String projectName) {
        this.name = projectName;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }
}
