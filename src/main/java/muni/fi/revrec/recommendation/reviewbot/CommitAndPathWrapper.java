package muni.fi.revrec.recommendation.reviewbot;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * CommitAndPathWrapper class is used to wrap the information about git commit and its file path into single object.
 *
 * @author Jakub Lipcak, Masaryk University
 */
public class CommitAndPathWrapper {
    private RevCommit revCommit;
    private String path;

    public CommitAndPathWrapper(RevCommit revCommit, String path) {
        this.revCommit = revCommit;
        this.path = path;
    }

    public RevCommit getRevCommit() {
        return revCommit;
    }

    public void setRevCommit(RevCommit revCommit) {
        this.revCommit = revCommit;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
