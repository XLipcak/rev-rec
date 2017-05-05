package muni.fi.revrec.model.pullRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.project.Project;
import muni.fi.revrec.model.reviewer.Developer;

import javax.persistence.*;
import java.util.Set;

/**
 * This class is used to represent the code change waiting for code review, independently of PullRequest class in api model.
 *
 * @author Jakub Lipcak, Masaryk University
 */
@Entity
public class PullRequest {
    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER)
    private Project project;

    private String subProject;

    private String changeId;
    private Integer changeNumber;
    private Long timestamp;

    private Integer insertions;
    private Integer deletions;

    @JoinTable(name="review")
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Developer> reviewer;

    @OneToOne(fetch = FetchType.EAGER)
    private Developer owner;

    @OneToMany(mappedBy = "pullRequest", fetch = FetchType.EAGER)
    private Set<FilePath> filePaths;

    public PullRequest() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getSubProject() {
        return subProject;
    }

    public void setSubProject(String subProject) {
        this.subProject = subProject;
    }

    public String getChangeId() {
        return changeId;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    public Integer getChangeNumber() {
        return changeNumber;
    }

    public void setChangeNumber(Integer changeNumber) {
        this.changeNumber = changeNumber;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long time) {
        this.timestamp = time;
    }

    public Integer getInsertions() {
        return insertions;
    }

    public void setInsertions(Integer insertions) {
        this.insertions = insertions;
    }

    public Integer getDeletions() {
        return deletions;
    }

    public void setDeletions(Integer deletions) {
        this.deletions = deletions;
    }

    public Set<Developer> getReviewer() {
        return reviewer;
    }

    public void setReviewer(Set<Developer> reviewers) {
        this.reviewer = reviewers;
    }

    public Developer getOwner() {
        return owner;
    }

    public void setOwner(Developer owner) {
        this.owner = owner;
    }

    public Set<FilePath> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(Set<FilePath> filePaths) {
        this.filePaths = filePaths;
    }

    /*public void setFilePaths(List<String> filePaths) {
        Set<FilePath> result = new HashSet<>();
        filePaths.forEach(x -> result.add(new FilePath(x)));
        this.filePaths = result;
    }*/
}
