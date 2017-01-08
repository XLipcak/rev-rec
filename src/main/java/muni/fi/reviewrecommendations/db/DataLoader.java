package muni.fi.reviewrecommendations.db;

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import muni.fi.reviewrecommendations.common.GerritBrowser;
import muni.fi.reviewrecommendations.common.GitBrowser;
import muni.fi.reviewrecommendations.db.model.filePath.FilePath;
import muni.fi.reviewrecommendations.db.model.filePath.FilePathDAO;
import muni.fi.reviewrecommendations.db.model.project.Project;
import muni.fi.reviewrecommendations.db.model.project.ProjectDAO;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequest;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestDAO;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import muni.fi.reviewrecommendations.db.model.reviewer.ReviewerDAO;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.FooterLine;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Service
public class DataLoader {
    @Autowired
    private ProjectDAO projectDAO;

    @Autowired
    private FilePathDAO filePathDAO;

    @Autowired
    private PullRequestDAO pullRequestDAO;

    @Autowired
    private ReviewerDAO reviewerDAO;

    public void loadData(GerritBrowser gerritBrowser, GitBrowser gitBrowser, String projectName) throws GitAPIException, RestApiException {
        Git git = gitBrowser.getGit();
        Project project = new Project();
        project.setProjectName(projectName);
        project = projectDAO.save(project);

        Iterable<RevCommit> commits = git.log().call();
        List<RevCommit> commitsList = Lists.newArrayList(commits);
        int counter = 0;
        int noEmptyListSizeCounter = 0;
        for (RevCommit commit : commitsList) {
            counter++;
            List<AccountInfo> accountInfos = getUserRelatedToCommit(commit, gerritBrowser);
            System.out.println(counter + "   " + getChangeIdFromFooter(commit.getFooterLines()) + "   " + accountInfos.size());
            if (accountInfos.size() > 0) {
                List<Reviewer> reviewers = new ArrayList<>();
                for (AccountInfo accountInfo : accountInfos) {
                    if (accountInfo.email == null) {
                        noEmptyListSizeCounter++;
                    } else {
                        Reviewer reviewer = reviewerDAO.findByEmail(accountInfo.email);
                        if (reviewer == null) {
                            reviewerDAO.save(new Reviewer(accountInfo.email, accountInfo.name));
                            reviewer = reviewerDAO.findByEmail(accountInfo.email);
                        }
                        reviewers.add(reviewer);
                    }
                }

                String changeId = getChangeIdFromFooter(commit.getFooterLines());
                PullRequest pullRequest = new PullRequest();
                pullRequest.setChangeId(changeId);
                pullRequest.setTime((long) commit.getCommitTime());
                pullRequest.setReviewers(new HashSet<>(reviewers));
                pullRequest.setProject(project);
                pullRequest = pullRequestDAO.save(pullRequest);

                List<String> paths = gerritBrowser.getFilePaths(changeId);
                for (String path : paths) {
                    FilePath filePath = new FilePath(path, pullRequest);
                    filePathDAO.save(filePath);
                }
            }

        }
        System.out.println("EntityExistsExceptions: " + noEmptyListSizeCounter);
    }


    private List<AccountInfo> getUserRelatedToCommit(RevCommit commit, GerritBrowser gerritBrowser) throws RestApiException {
        String changeId = getChangeIdFromFooter(commit.getFooterLines());
        if (changeId.equals("")) {
            return new ArrayList<>();
        }
        return (List<AccountInfo>) gerritBrowser.getReviewers(changeId);
    }

    private String getChangeIdFromFooter(List<FooterLine> commitFooter) {
        if (commitFooter.size() == 0) {
            return "";
        }
        return commitFooter.get(0).getValue();
    }

}


