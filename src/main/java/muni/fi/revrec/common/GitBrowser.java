package muni.fi.revrec.common;

import muni.fi.revrec.recommendation.reviewbot.CommitAndPathWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.RawParseUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
@Service
public class GitBrowser {

    private boolean FOLLOW_MERGED_COMMITS = false;

    private String projectRepositoryPath;

    private final Log logger = LogFactory.getLog(this.getClass());


    public GitBrowser(@Value("${recommendation.project}") String project) throws IOException {
        this.projectRepositoryPath = "repos/" + project;
    }


    public List<CommitAndPathWrapper> getFileCommitHistory(String filePath, String subProjectName) {
        if (FOLLOW_MERGED_COMMITS) {
            return getFileCommitHistoryWithRenames(filePath, subProjectName);
        } else {
            List<CommitAndPathWrapper> result = new ArrayList<>();
            for (RevCommit revCommit : getFileCommitHistoryWithoutRenames(filePath, subProjectName)) {
                result.add(new CommitAndPathWrapper(revCommit, filePath));
            }
            return result;
        }
    }

    private List<RevCommit> getFileCommitHistoryWithoutRenames(String filePath, String subProjectName) {
        Iterable<RevCommit> logMsgs = null;

        try {
            Git git = Git.open(new File(projectRepositoryPath + "/" + subProjectName));
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
    private ArrayList<CommitAndPathWrapper> getFileCommitHistoryWithRenames(String filePath, String subProjectName) {
        ArrayList<CommitAndPathWrapper> commits = new ArrayList<CommitAndPathWrapper>();
        RevCommit start = null;

        //TODO: reimplement git log --follow
        int maxRenames = 1;
        int renamesCounter = 0;

        try {
            Git git = Git.open(new File(projectRepositoryPath + "/" + subProjectName));
            do {
                Iterable<RevCommit> log = git.log().addPath(filePath).call();
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
            while ((filePath = getRenamedPath(start, filePath, git)) != null);
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
    private String getRenamedPath(RevCommit start, String filePath, Git git) throws IOException, MissingObjectException, GitAPIException {
        Iterable<RevCommit> allCommitsLater = git.log().add(start).call();
        for (RevCommit commit : allCommitsLater) {

            TreeWalk tw = new TreeWalk(git.getRepository());
            tw.addTree(commit.getTree());
            tw.addTree(start.getTree());
            tw.setRecursive(true);
            //tw.setFilter(TreeFilter.);
            RenameDetector rd = new RenameDetector(git.getRepository());
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

    public EditList diff(RevCommit headCommit, RevCommit diffWith, String filePath, String subProjectName) throws IOException {
        OutputStream outputStream = System.out; //DisabledOutputStream.INSTANCE;
        DiffFormatter formatter = new DiffFormatter(outputStream);

        Git git = Git.open(new File(projectRepositoryPath + "/" + subProjectName));
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

    public int getNearestLineOfCode(RevCommit headCommit, String filePath, int lineNumber, String subProjectName) throws IOException {
        //to be changed

        String[] lines;
        try {
            Git git = Git.open(new File(projectRepositoryPath + "/" + subProjectName));
            Repository repository = git.getRepository();
            RevTree tree = headCommit.getTree();

            // try to find a specific file
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filePath));
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);

            // get its content and find nearest line longer than 1
            byte[] contents = loader.getCachedBytes();
            String file = RawParseUtils.decode(contents);
            lines = file.split("\n");

            for (int x = 1; x < lines.length; x++) {
                if (lines[lineNumber - 1 - x].length() > 1 || (lines[lineNumber - 1 - x].length() == 1 && !Character.isWhitespace(lines[lineNumber - 1 - x].charAt(0)))) {
                    return lineNumber - x;
                }
                if (lines[lineNumber - 1 + x].length() > 1 || (lines[lineNumber - 1 - x].length() == 1 && !Character.isWhitespace(lines[lineNumber - 1 - x].charAt(0)))) {
                    return lineNumber + x;
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());

            //solved by considering the line below as the nearest line
            return lineNumber - 1;
        }

        return lineNumber;
    }
}
