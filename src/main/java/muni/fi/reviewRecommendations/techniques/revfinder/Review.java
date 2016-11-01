package muni.fi.reviewRecommendations.techniques.revfinder;

import java.util.Date;

/**
 *
 * @author Jakub Lipcak, Masaryk University
 */
public class Review {

    private String[] filePaths;
    private String[] reviewers;
    private Date date;

    public Review() {
    }

    public String[] getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(String[] filePaths) {
        this.filePaths = filePaths;
    }

    public String[] getReviewers() {
        return reviewers;
    }

    public void setReviewers(String[] reviewers) {
        this.reviewers = reviewers;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
