package muni.fi.reviewrecommendations.techniques.reviewbot;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Created by Kubo on 10.12.2016.
 */
public class CommitAndPathWrapper {
    private RevCommit revCommit;
    private String path;

    public CommitAndPathWrapper() {
    }

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
