package muni.fi.revrec.common;

import muni.fi.revrec.recommendation.reviewbot.CommitAndPathWrapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides data from local GIT repositories.
 *
 * @author Jakub Lipcak, Masaryk University
 */
public class GitBrowser {

    private boolean followFlag;
    private Git git;
    private Repository repository;


    public GitBrowser(String repositoryPath, boolean followFlag) throws IOException {
        this.followFlag = followFlag;
        //TODO: support for local and remote repositories
        this.git = Git.open(new File(repositoryPath));
        this.repository = git.getRepository();
    }


    public List<CommitAndPathWrapper> getFileCommitHistory(String filePath) {
        if (followFlag) {
            return getFileCommitHistoryWithRenames(filePath);
        } else {
            List<CommitAndPathWrapper> result = new ArrayList<>();
            for (RevCommit revCommit : getFileCommitHistoryWithoutRenames(filePath)) {
                result.add(new CommitAndPathWrapper(revCommit, filePath));
            }
            return result;
        }
    }

    private List<RevCommit> getFileCommitHistoryWithoutRenames(String filePath) {
        Iterable<RevCommit> logMsgs = null;

        try {
            LogCommand log = git.log().addPath(filePath);
            logMsgs = log.call();
        } catch (Exception e) {
            //TODO: Handle Exceptions separately
            System.out.println("no head exception : " + e);
        }

        List<RevCommit> result = new ArrayList<>();
        logMsgs.forEach(result::add);

        return result;
    }

    /**
     * Returns the result of a git log --follow -- < path >
     */
    private ArrayList<CommitAndPathWrapper> getFileCommitHistoryWithRenames(String filePath) {
        ArrayList<CommitAndPathWrapper> commits = new ArrayList<CommitAndPathWrapper>();
        RevCommit start = null;

        //TODO: reimplement git log --follow
        int maxRenames = 1;
        int renamesCounter = 0;

        try {
            do {

                Iterable<RevCommit> log = git.log().addPath(filePath).call();
                //List<RevCommit> testList = Lists.newArrayList(log);
                for (RevCommit commit : log) {
                    if (commits.contains(commit)) {
                        start = null;
                    } else {
                        start = commit;
                        commits.add(new CommitAndPathWrapper(commit, filePath));
                    }
                }
                if (start == null) return commits;
                if (renamesCounter == maxRenames) {
                    return commits;
                }
                renamesCounter++;
            }
            while ((filePath = getRenamedPath(start, filePath)) != null);
        } catch (Exception e) {
            //TODO: Handle Exceptions separately
            System.out.println("no head exception : " + e);
        }

        return commits;
    }

    /**
     * Checks for renames in history of a certain file. Returns null, if no rename was found.
     * Can take some seconds, especially if nothing is found...
     */
    private String getRenamedPath(RevCommit start, String filePath) throws IOException, MissingObjectException, GitAPIException {
        Iterable<RevCommit> allCommitsLater = git.log().add(start).call();
        for (RevCommit commit : allCommitsLater) {

            TreeWalk tw = new TreeWalk(repository);
            tw.addTree(commit.getTree());
            tw.addTree(start.getTree());
            tw.setRecursive(true);
            //tw.setFilter(TreeFilter.);
            RenameDetector rd = new RenameDetector(repository);
            rd.addAll(DiffEntry.scan(tw));
            List<DiffEntry> files = rd.compute();
            for (DiffEntry diffEntry : files) {
                if ((diffEntry.getChangeType() == DiffEntry.ChangeType.RENAME ||
                        diffEntry.getChangeType() == DiffEntry.ChangeType.COPY) &&
                        diffEntry.getNewPath().contains(filePath)) {
                    System.out.println("Found: " + diffEntry.toString() + " return " + diffEntry.getOldPath());
                    return diffEntry.getOldPath();
                }
            }
        }
        return null;
    }

    public void printCommitListInfo(Iterable<RevCommit> logMsgs) {
        int commitsCounter = 0;

        for (RevCommit commit : logMsgs) {
            commitsCounter++;
            System.out.println("----------------------------------------");
            System.out.println(commit);
            System.out.println("Tree: " + commit.getTree());
            System.out.println(commit.getAuthorIdent().getName());
            System.out.println(commit.getAuthorIdent().getWhen());
            System.out.println(" ---- " + commit.getFullMessage());
            System.out.println("----------------------------------------");
        }

        System.out.println("Commits: " + commitsCounter);
    }

    public EditList diff(RevCommit headCommit, RevCommit diffWith, String filePath) throws IOException {
        OutputStream outputStream = System.out; //DisabledOutputStream.INSTANCE;
        DiffFormatter formatter = new DiffFormatter(outputStream);
        formatter.setRepository(git.getRepository());

        if (filePath != null) {
            formatter.setPathFilter(PathFilter.create(filePath));
        }

        List<DiffEntry> entries = formatter.scan(diffWith, headCommit);
        //formatter.format(entries.get(0)); //to be deleted
        if (entries.size() > 0) {
            FileHeader fileHeader = formatter.toFileHeader(entries.get(0));
            EditList edits = fileHeader.toEditList();

            return edits;
        } else {
            return new EditList();
        }

    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public boolean isFollowFlag() {
        return followFlag;
    }

    public void setFollowFlag(boolean followFlag) {
        this.followFlag = followFlag;
    }

    public Git getGit() {
        return git;
    }

    public void setGit(Git git) {
        this.git = git;
    }
}
