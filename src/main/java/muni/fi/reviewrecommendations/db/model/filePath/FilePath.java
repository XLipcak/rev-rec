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
    private String filePath;
    @ManyToOne(fetch = FetchType.EAGER)
    private PullRequest pullRequest;

    public FilePath() {
    }

    public FilePath(String filePath, PullRequest pullRequest) {
        this.filePath = filePath;
        this.pullRequest = pullRequest;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }
}
