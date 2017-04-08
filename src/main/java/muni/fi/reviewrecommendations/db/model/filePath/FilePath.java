package muni.fi.reviewrecommendations.db.model.filePath;

import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequest;

import javax.persistence.*;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Entity
public class FilePath {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Integer id;
    private String location;

    @ManyToOne(fetch = FetchType.EAGER)
    private PullRequest pullRequest;

    public FilePath() {
    }

    public FilePath(String location) {
        this.location = location;
    }

    public FilePath(String location, PullRequest pullRequest) {
        this.location = location;
        this.pullRequest = pullRequest;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String filePath) {
        this.location = filePath;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }
}
