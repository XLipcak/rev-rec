package muni.fi.revrec.model.filePath;

import com.fasterxml.jackson.annotation.JsonIgnore;
import muni.fi.revrec.model.pullRequest.PullRequest;

import javax.persistence.*;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Entity
@Table(name = "filepath")
public class FilePath {
    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Integer id;

    @Column(length = 1024)
    private String location;

    @JsonIgnore
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
