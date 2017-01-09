package muni.fi.reviewrecommendations.db.model.pullRequest;

import muni.fi.reviewrecommendations.db.model.filePath.FilePath;
import muni.fi.reviewrecommendations.db.model.project.Project;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;

import javax.persistence.*;
import java.util.Set;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Entity
public class PullRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Integer id;

    private String changeId;
    private Integer changeNumber;
    private Long time;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Reviewer> reviewers;

    @OneToMany(mappedBy = "pullRequest", fetch = FetchType.EAGER)
    private Set<FilePath> filePaths;

    @OneToOne(fetch = FetchType.EAGER)
    private Project project;

    public PullRequest() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getChangeId() {
        return changeId;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Set<Reviewer> getReviewers() {
        return reviewers;
    }

    public void setReviewers(Set<Reviewer> reviewers) {
        this.reviewers = reviewers;
    }

    public Set<FilePath> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(Set<FilePath> filePaths) {
        this.filePaths = filePaths;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Integer getChangeNumber() {
        return changeNumber;
    }

    public void setChangeNumber(Integer changeNumber) {
        this.changeNumber = changeNumber;
    }
}
