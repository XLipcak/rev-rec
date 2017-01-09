package muni.fi.reviewrecommendations.db.model.project;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Entity
public class Project {
    @Id
    private String projectName;
    private int pullRequestsCount;

    public Project() {
    }

    public Project(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public int getPullRequestsCount() {
        return pullRequestsCount;
    }

    public void setPullRequestsCount(int pullRequestsCount) {
        this.pullRequestsCount = pullRequestsCount;
    }
}
