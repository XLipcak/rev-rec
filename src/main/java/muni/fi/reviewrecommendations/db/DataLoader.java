package muni.fi.reviewrecommendations.db;

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
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

    public void loadDataFromGerritAndGit(GerritBrowser gerritBrowser, GitBrowser gitBrowser, String projectName) throws GitAPIException {
        int counter = 0;
        Git git = gitBrowser.getGit();
        Project project = new Project();
        if (projectDAO.findOne(projectName) != null) {
            project = projectDAO.findOne(projectName);
            counter = project.getPullRequestsCount();
        } else {
            project.setProjectName(projectName);
            project.setPullRequestsCount(0);
            project = projectDAO.save(project);
        }

        Iterable<RevCommit> commits = git.log().call();
        List<RevCommit> commitsList = Lists.newArrayList(commits);
        int noEmptyListSizeCounter = 0;
        try {
            for (RevCommit commit : commitsList) {
                String changeId = getChangeIdFromFooter(commit.getFooterLines());
                if (pullRequestDAO.findByChangeId(changeId).size() > 0) {
                    continue;
                }
                List<AccountInfo> accountInfos = getUserRelatedToCommit(commit, gerritBrowser);
                System.out.println(counter + "   " + changeId + "   " + accountInfos.size());
                if (accountInfos.size() > 0) {
                    List<Reviewer> reviewers = new ArrayList<>();
                    for (AccountInfo accountInfo : accountInfos) {
                        if (accountInfo.email == null) {
                            noEmptyListSizeCounter++;
                        } else {
                            Reviewer reviewer = reviewerDAO.findByEmail(accountInfo.email);
                            if (reviewer == null) {
                                Reviewer newReviewer = new Reviewer();
                                newReviewer.setId(accountInfo._accountId);
                                newReviewer.setEmail(accountInfo.email);
                                newReviewer.setName(accountInfo.name);
                                if(accountInfo.avatars.size() > 0){
                                    newReviewer.setAvatar(accountInfo.avatars.get(0).url);
                                }
                                reviewerDAO.save(newReviewer);
                                reviewer = reviewerDAO.findByEmail(accountInfo.email);
                            }
                            reviewers.add(reviewer);
                        }
                    }

                    PullRequest pullRequest = new PullRequest();
                    pullRequest.setChangeId(changeId);
                    pullRequest.setTime(gerritBrowser.getChange(changeId).created.getTime());
                    pullRequest.setReviewers(new HashSet<>(reviewers));
                    pullRequest.setProject(project);
                    pullRequest.setChangeNumber(gerritBrowser.getChangeNumber(changeId));
                    pullRequest = pullRequestDAO.save(pullRequest);

                    List<String> paths = gerritBrowser.getFilePaths(changeId);
                    for (String path : paths) {
                        FilePath filePath = new FilePath(path, pullRequest);
                        filePathDAO.save(filePath);
                    }
                }
                counter++;
            }
        } catch (Exception ex) {
            project.setPullRequestsCount(counter);
            System.out.println(ex);
        }
        project.setPullRequestsCount(counter);
        projectDAO.save(project);
        System.out.println("EntityExistsExceptions: " + noEmptyListSizeCounter);
    }

    public void loadDataFromGerrit(GerritBrowser gerritBrowser, String projectName) {
        int counter = 0;
        Project project = new Project();
        if (projectDAO.findOne(projectName) != null) {
            project = projectDAO.findOne(projectName);
            counter = project.getPullRequestsCount();
        } else {
            project.setProjectName(projectName);
            project.setPullRequestsCount(0);
            project = projectDAO.save(project);
        }

        try {
            List<ChangeInfo> changeInfos = null;
            do {
                changeInfos = gerritBrowser.getGerritChanges(counter);
                for (ChangeInfo changeInfo : changeInfos) {
                    counter++;
                    String changeId = changeInfo.changeId;
                    if (pullRequestDAO.findByChangeId(changeId).size() > 0) {
                        continue;
                    }
                    if(!gerritBrowser.getChange(changeId).project.equals("platform/sdk")){
                        continue;
                    }
                    List<AccountInfo> accountInfos = Lists.newArrayList(gerritBrowser.getReviewers(changeId));
                    if (accountInfos.size() == 0) {
                        continue;
                    }
                    System.out.println(counter + "   " + changeId + "   " + accountInfos.size());
                    List<Reviewer> reviewers = new ArrayList<>();
                    for (AccountInfo accountInfo : accountInfos) {
                        if (accountInfo.email == null) {
                        } else {
                            Reviewer reviewer = reviewerDAO.findByEmail(accountInfo.email);
                            if (reviewer == null) {
                                Reviewer newReviewer = new Reviewer();
                                newReviewer.setId(accountInfo._accountId);
                                newReviewer.setEmail(accountInfo.email);
                                newReviewer.setName(accountInfo.name);
                                if(accountInfo.avatars.size() > 0){
                                    newReviewer.setAvatar(accountInfo.avatars.get(0).url);
                                }
                                reviewerDAO.save(newReviewer);
                                reviewer = reviewerDAO.findByEmail(accountInfo.email);
                            }
                            reviewers.add(reviewer);
                        }
                    }

                    PullRequest pullRequest = new PullRequest();
                    pullRequest.setChangeId(changeId);
                    pullRequest.setTime(changeInfo.created.getTime());
                    pullRequest.setReviewers(new HashSet<>(reviewers));
                    pullRequest.setProject(project);
                    pullRequest.setChangeNumber(gerritBrowser.getChangeNumber(changeId));
                    pullRequest = pullRequestDAO.save(pullRequest);

                    List<String> paths = gerritBrowser.getFilePaths(changeId);
                    for (String path : paths) {
                        FilePath filePath = new FilePath(path, pullRequest);
                        filePathDAO.save(filePath);
                    }
                }
            }
            while (changeInfos.get(changeInfos.size() - 1)._moreChanges != null && changeInfos.get(changeInfos.size() - 1)._moreChanges);
        } catch (RestApiException ex) {
            System.out.println(ex);
        }
        project.setPullRequestsCount(counter);
        projectDAO.save(project);
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


