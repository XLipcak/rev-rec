package muni.fi.revrec.common;

import muni.fi.revrec.common.exception.ReviewerRecommendationException;
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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.jgit.util.io.DisabledOutputStream;
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
public class GitService {

    private boolean FOLLOW_RENAMED_FILES = true;

    private String projectRepositoryPath;

    private final Log logger = LogFactory.getLog(this.getClass());


    public GitService(@Value("${recommendation.project}") String project) throws IOException {
        this.projectRepositoryPath = "repos/" + project;
    }


    /**
     * Returns the file commit history of the specified file. Tracking of renames depends on the value
     * of FOLLOW_RENAMED_FILES variable.
     *
     * @param filePath       location of the file.
     * @param subProjectName name of the sub-project (name of the repository).
     * @return list of previous commits and file locations of the specified file.
     */
    public List<CommitAndPathWrapper> getFileCommitHistory(String filePath, String subProjectName) {
        if (FOLLOW_RENAMED_FILES) {
            return getFileCommitHistoryWithRenames(filePath, subProjectName);
        } else {
            List<CommitAndPathWrapper> result = new ArrayList<>();
            for (RevCommit revCommit : getFileCommitHistoryWithoutRenames(filePath, subProjectName)) {
                result.add(new CommitAndPathWrapper(revCommit, filePath));
            }
            return result;
        }
    }

    /**
     * Returns the file commit history, but files are only tracked, until they haven't been renamed.
     *
     * @param filePath       location of the file.
     * @param subProjectName name of the sub-project (name of the repository).
     * @return list of previous commits and file locations of the specified file (no renames are tracked).
     */
    private List<RevCommit> getFileCommitHistoryWithoutRenames(String filePath, String subProjectName) {
        Iterable<RevCommit> logMsgs = new ArrayList<>();

        try {
            Git git = Git.open(new File(projectRepositoryPath + "/" + subProjectName));
            LogCommand log = git.log().addPath(filePath);
            logMsgs = log.call();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            throw new ReviewerRecommendationException(ex.getMessage());
        }

        List<RevCommit> result = new ArrayList<>();
        logMsgs.forEach(result::add);

        return result;
    }

    /**
     * Returns the result of a git log --follow -- < path > command.
     *
     * @param filePath       location of the file.
     * @param subProjectName name of the sub-project (name of the repository).
     * @return list of all previous commits and file locations of the specified file.
     */
    private ArrayList<CommitAndPathWrapper> getFileCommitHistoryWithRenames(String filePath, String subProjectName) {
        ArrayList<CommitAndPathWrapper> commits = new ArrayList<CommitAndPathWrapper>();
        RevCommit start = null;

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
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            throw new ReviewerRecommendationException(ex.getMessage());
        }

        return commits;
    }

    /**
     * Checks for renames in history of a certain file.
     *
     * @param start    head commit.
     * @param filePath location of the file.
     * @param git      git instance.
     * @return null, if no rename was found, old file's name otherwise.
     * @throws IOException     in case of problems with reading the repository.
     * @throws GitAPIException in case of problems with reading the repository.
     */
    private String getRenamedPath(RevCommit start, String filePath, Git git) throws IOException, GitAPIException {
        Iterable<RevCommit> allCommitsLater = git.log().add(start).call();
        for (RevCommit commit : allCommitsLater) {

            TreeWalk tw = new TreeWalk(git.getRepository());
            tw.addTree(commit.getTree());
            tw.addTree(start.getTree());
            tw.setRecursive(true);
            RenameDetector rd = new RenameDetector(git.getRepository());
            rd.addAll(DiffEntry.scan(tw));
            List<DiffEntry> files = rd.compute();
            for (DiffEntry diffEntry : files) {
                if ((diffEntry.getChangeType() == DiffEntry.ChangeType.RENAME ||
                        diffEntry.getChangeType() == DiffEntry.ChangeType.COPY) &&
                        diffEntry.getNewPath().contains(filePath)) {
                    logger.debug("Found: " + diffEntry.toString() + " return " + diffEntry.getOldPath());
                    return diffEntry.getOldPath();
                }
            }
        }
        return null;
    }

    /**
     * Find list of changes of specified file between two particular commits.
     *
     * @param headCommit     head commit.
     * @param diffWith       commit to be compared with head commit.
     * @param filePath       location of the file.
     * @param subProjectName name of the sub-project (name of the repository).
     * @return list of changes.
     * @throws IOException in case of problems with reading the repository.
     */
    public EditList diff(RevCommit headCommit, RevCommit diffWith, String filePath, String subProjectName) throws IOException {
        OutputStream outputStream = DisabledOutputStream.INSTANCE;
        DiffFormatter formatter = new DiffFormatter(outputStream);

        Git git = Git.open(new File(projectRepositoryPath + "/" + subProjectName));
        formatter.setRepository(git.getRepository());

        if (filePath != null) {
            formatter.setPathFilter(PathFilter.create(filePath));
        }

        List<DiffEntry> entries = formatter.scan(diffWith, headCommit);
        if (entries.size() > 0) {
            FileHeader fileHeader = formatter.toFileHeader(entries.get(0));
            return fileHeader.toEditList();
        } else {
            return new EditList();
        }

    }

    /**
     * Get the nearest line of code in the particular file at specific commit.
     *
     * @param headCommit     repository state to be checked.
     * @param filePath       location of the file.
     * @param lineNumber     number of the line to be checked.
     * @param subProjectName name of the sub-project (name of the repository).
     * @return nearest line of source code in the file.
     * @throws IOException in case of problems with reading the repository.
     */
    public int getNearestLineOfCode(RevCommit headCommit, String filePath, int lineNumber, String subProjectName) throws IOException {
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
            //consider the line below as the nearest line
            return lineNumber - 1;
        }

        return lineNumber;
    }
}
