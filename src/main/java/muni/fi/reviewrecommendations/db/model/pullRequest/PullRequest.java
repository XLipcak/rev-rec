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

    private Integer insertions;
    private Integer deletions;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Reviewer> codeReviewers;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Reviewer> verifiedReviewers;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Reviewer> allReviewers;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Reviewer> allCommentators;

    //subset of allCodeReviewers
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Reviewer> allSpecificCodeReviewers;

    @OneToMany(mappedBy = "pullRequest", fetch = FetchType.EAGER)
    private Set<FilePath> filePaths;

    @OneToOne(fetch = FetchType.EAGER)
    private Project project;

    @OneToOne(fetch = FetchType.EAGER)
    private Reviewer owner;

    private String subProject;

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

    public String getSubProject() {
        return subProject;
    }

    public void setSubProject(String subProject) {
        this.subProject = subProject;
    }

    public Reviewer getOwner() {
        return owner;
    }

    public void setOwner(Reviewer owner) {
        this.owner = owner;
    }

    public Set<Reviewer> getCodeReviewers() {
        return codeReviewers;
    }

    public void setCodeReviewers(Set<Reviewer> codeReviewers) {
        this.codeReviewers = codeReviewers;
    }

    public Set<Reviewer> getVerifiedReviewers() {
        return verifiedReviewers;
    }

    public void setVerifiedReviewers(Set<Reviewer> verifiedReviewers) {
        this.verifiedReviewers = verifiedReviewers;
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

    public Set<Reviewer> getAllReviewers() {
        return allReviewers;
    }

    public void setAllReviewers(Set<Reviewer> allReviewers) {
        this.allReviewers = allReviewers;
    }

    public Set<Reviewer> getAllCommentators() {
        return allCommentators;
    }

    public void setAllCommentators(Set<Reviewer> allCommentators) {
        this.allCommentators = allCommentators;
    }

    public Set<Reviewer> getAllSpecificCodeReviewers() {
        return allSpecificCodeReviewers;
    }

    public void setAllSpecificCodeReviewers(Set<Reviewer> allSpecificCodeReviewers) {
        this.allSpecificCodeReviewers = allSpecificCodeReviewers;
    }
}
