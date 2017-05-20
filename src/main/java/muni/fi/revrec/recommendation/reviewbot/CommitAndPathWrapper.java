package muni.fi.revrec.recommendation.reviewbot;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * CommitAndPathWrapper class is used to wrap the information about git commit and its file path into the single object.
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

    public String getPath() {
        return path;
    }
}
