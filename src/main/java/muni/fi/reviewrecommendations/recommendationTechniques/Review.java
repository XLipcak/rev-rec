package muni.fi.reviewrecommendations.recommendationTechniques;

import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;

import java.util.Date;
import java.util.List;

/**
 * @author Jakub Lipcak, Masaryk University
 */
public class Review {

    private List<String> filePaths;
    private List<Reviewer> reviewers;
    private Date date;

    public Review() {
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    public List<Reviewer> getReviewers() {
        return reviewers;
    }

    public void setReviewers(List<Reviewer> reviewers) {
        this.reviewers = reviewers;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
