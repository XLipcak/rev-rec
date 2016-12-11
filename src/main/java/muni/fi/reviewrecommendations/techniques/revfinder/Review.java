package muni.fi.reviewrecommendations.techniques.revfinder;

import java.util.Date;
import java.util.List;

/**
 *
 * @author Jakub Lipcak, Masaryk University
 */
public class Review {

    private List<String> filePaths;
    private String[] reviewers;
    private Date date;

    public Review() {
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
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
