package muni.fi.revrec.common.data;

import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.reviewer.Developer;

import java.util.Set;

public class GitHubPullRequestParser implements PullRequestParser {
    @Override
    public Set<FilePath> getFilePaths() {
        return null;
    }

    @Override
    public String getChangeId() {
        return null;
    }

    @Override
    public Integer getChangeNumber() {
        return null;
    }

    @Override
    public Developer getOwner() {
        return null;
    }

    @Override
    public String getSubProject() {
        return null;
    }

    @Override
    public Long getTimeStamp() {
        return null;
    }

    @Override
    public Integer getInsertions() {
        return null;
    }

    @Override
    public Integer getDeletions() {
        return null;
    }
}
