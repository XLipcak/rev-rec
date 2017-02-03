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
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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

    /**
     * Extract data from JSON file and store them in the database. Only information about reviewers is fetched from Gerrit.
     *
     * @param fileLocation location of JSON file
     * @param gerritBrowser instance of Gerrit browser for chosen project
     * @param projectName name of the project
     * @throws RestApiException
     * @throws ParseException
     */
    public void loadDataFromFile(String fileLocation, GerritBrowser gerritBrowser, String projectName) throws RestApiException, ParseException {
        try (BufferedReader br = new BufferedReader(new FileReader(fileLocation))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            int x = 0;

            Project project = new Project();
            if (projectDAO.findOne(projectName) != null) {
                project = projectDAO.findOne(projectName);
            } else {
                project.setName(projectName);
                project = projectDAO.save(project);
            }

            while (line != null) {
                x++;
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();

                if (line == null) {
                    return;
                } else {
                    System.out.println(x);
                    JSONObject obj = new JSONObject(line);

                    Set<Reviewer> reviewers = getSetOfReviewersFromJsonObject(obj, gerritBrowser);

                    PullRequest pullRequest = new PullRequest();
                    pullRequest.setChangeId(Integer.toString((Integer) obj.get("changeId")));
                    pullRequest.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse((String) obj.get("submit_date")).getTime());

                    pullRequest.setCodeReviewers(reviewers);

                    pullRequest.setProject(project);
                    //pullRequest.setChangeNumber(gerritBrowser.getChangeNumber(Integer.toString((Integer) obj.get("changeId"))));
                    pullRequest = pullRequestDAO.save(pullRequest);


                    JSONArray arr = obj.getJSONArray("files");
                    for (int i = 0; i < arr.length(); i++) {
                        FilePath filePath = new FilePath();
                        filePath.setFilePath((String) arr.get(i));
                        filePath.setPullRequest(pullRequest);
                        filePathDAO.save(filePath);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract change IDs from JSON file and fetch all information about these change requests from Gerrit.
     *
     * @param fileLocation location of JSON file
     * @param gerritBrowser instance of Gerrit browser for chosen project
     * @param projectName name of the project
     * @throws RestApiException
     * @throws ParseException
     */
    public void loadDataByChangeIdsFromFile(String fileLocation, GerritBrowser gerritBrowser, String projectName) throws RestApiException, ParseException {
        try (BufferedReader br = new BufferedReader(new FileReader(fileLocation))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            int x = 0;

            while (line != null) {
                x++;
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();

                if (line == null) {
                    return;
                } else {
                    if (x > 4754) {
                        System.out.println(x);
                        JSONObject obj = new JSONObject(line);
                        String changeId = Integer.toString((Integer) obj.get("changeId"));
                        ChangeInfo changeInfo = gerritBrowser.getChange(changeId);
                        saveChangeRequest(changeInfo, projectName, gerritBrowser, getSetOfReviewersFromJsonObject(obj, gerritBrowser));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract all previous pull requests from the information in GIT repository and store them in the database,
     *
     * @param gerritBrowser instance of Gerrit browser for chosen project
     * @param gitBrowser instance of Git browser for chosen project
     * @param projectName name of the project
     * @throws GitAPIException
     */
    public void loadDataFromGit(GerritBrowser gerritBrowser, GitBrowser gitBrowser, String projectName) throws GitAPIException {
        int counter = 0;
        Git git = gitBrowser.getGit();

        Iterable<RevCommit> commits = git.log().call();
        List<RevCommit> commitsList = Lists.newArrayList(commits);

        for (RevCommit commit : commitsList) {
            try {
                String changeId = getChangeIdFromFooter(commit.getFooterLines());
                if (pullRequestDAO.findByChangeIdAndProjectName(changeId, projectName).size() > 0) {
                    continue;
                }

                List<AccountInfo> accountInfos = getUserRelatedToCommit(commit, gerritBrowser);
                System.out.println(counter + "   " + changeId + "   " + accountInfos.size());

                if (accountInfos.isEmpty()) {
                    continue;
                }

                ChangeInfo changeInfo = gerritBrowser.getChange(changeId);
                saveChangeRequest(changeInfo, projectName, gerritBrowser);

                counter++;
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
    }

    private void saveChangeRequest(ChangeInfo changeInfo, String projectName, GerritBrowser gerritBrowser) throws RestApiException {
        saveChangeRequest(changeInfo, projectName, gerritBrowser, new HashSet<>());
    }

    private void saveChangeRequest(ChangeInfo changeInfo, String projectName, GerritBrowser gerritBrowser, Set<Reviewer> specificCodeReviewers) throws RestApiException {
        Project project = new Project();
        if (projectDAO.findOne(projectName) != null) {
            project = projectDAO.findOne(projectName);
        } else {
            project.setName(projectName);
            project = projectDAO.save(project);
        }

        if (changeInfo.reviewers == null) {
            System.out.println("NULL");
            return;
        }

        Set<Reviewer> allReviewers = new HashSet<>();
        for (AccountInfo accountInfo : changeInfo.reviewers.values().iterator().next()) {
            Reviewer reviewer = reviewerDAO.findOne(accountInfo._accountId);
            if (reviewer == null) {
                reviewer = new Reviewer(accountInfo);
                reviewerDAO.save(reviewer);
            }
            allReviewers.add(reviewer);
        }

        Reviewer owner = reviewerDAO.findOne(changeInfo.owner._accountId);
        if (owner == null) {
            owner = new Reviewer(changeInfo.owner);
            owner = reviewerDAO.save(owner);
        }

        PullRequest pullRequest = new PullRequest();
        pullRequest.setChangeId(changeInfo.changeId);
        pullRequest.setTime(changeInfo.created.getTime());
        pullRequest.setProject(project);
        pullRequest.setChangeNumber(changeInfo._number);
        pullRequest.setSubProject(changeInfo.project);
        pullRequest.setOwner(owner);
        pullRequest.setInsertions(changeInfo.insertions);
        pullRequest.setDeletions(changeInfo.deletions);

        pullRequest.setCodeReviewers(getSetOfReviewersInDbFromCollectionOfAccountInfos(gerritBrowser.getReviewers(changeInfo, "Code-Review")));
        pullRequest.setVerifiedReviewers(getSetOfReviewersInDbFromCollectionOfAccountInfos(gerritBrowser.getReviewers(changeInfo, "Verified")));
        pullRequest.setAllReviewers(allReviewers);
        pullRequest.setAllCommentators(getSetOfReviewersInDbFromCollectionOfAccountInfos(gerritBrowser.getCommentators(changeInfo)));
        pullRequest.setAllSpecificCodeReviewers(specificCodeReviewers);

        pullRequest = pullRequestDAO.save(pullRequest);

        List<String> paths = gerritBrowser.getFilePaths(changeInfo.changeId);
        for (String path : paths) {
            FilePath filePath = new FilePath(path, pullRequest);
            filePathDAO.save(filePath);
        }

    }

    //fetch, save and return all reviewers specified in JSONObject
    private Set<Reviewer> getSetOfReviewersFromJsonObject(JSONObject obj, GerritBrowser gerritBrowser) throws RestApiException {
        JSONArray arr = obj.getJSONArray("approve_history");
        Set<Reviewer> reviewers = new HashSet<>();
        for (int i = 0; i < arr.length(); i++) {
            Integer reviewerId = ((Integer) ((JSONObject) arr.get(i)).get("userId"));
            Reviewer reviewer = reviewerDAO.findOne(reviewerId);
            if (reviewer == null) {
                AccountInfo accountInfo = gerritBrowser.getAccount(reviewerId.toString());
                Reviewer newReviewer = new Reviewer();
                newReviewer.setId(accountInfo._accountId);
                newReviewer.setEmail(accountInfo.email);
                newReviewer.setName(accountInfo.name);
                if (accountInfo.avatars.size() > 0) {
                    newReviewer.setAvatar(accountInfo.avatars.get(0).url);
                }
                reviewer = reviewerDAO.save(newReviewer);
            }
            reviewers.add(reviewer);
        }
        return reviewers;
    }

    //fetch, save and return all reviewers specified in Collection<AccountInfo>
    private Set<Reviewer> getSetOfReviewersInDbFromCollectionOfAccountInfos(Collection<AccountInfo> accountInfos) {
        Set<Reviewer> result = new HashSet<>();
        for (AccountInfo accountInfo : accountInfos) {
            if (accountInfo != null) {
                Reviewer reviewer = reviewerDAO.findOne(accountInfo._accountId);
                if (reviewer == null) {
                    reviewer = reviewerDAO.save(new Reviewer(accountInfo));
                }
                result.add(reviewer);
            }
        }
        return result;
    }

    private List<AccountInfo> getUserRelatedToCommit(RevCommit commit, GerritBrowser gerritBrowser) throws RestApiException {
        String changeId = getChangeIdFromFooter(commit.getFooterLines());
        if (changeId.equals("")) {
            return new ArrayList<>();
        }
        return (List<AccountInfo>) gerritBrowser.getReviewers(changeId, "Code-Review");
    }

    private String getChangeIdFromFooter(List<FooterLine> commitFooter) {
        if (commitFooter.size() == 0) {
            return "";
        }
        return commitFooter.get(0).getValue();
    }

}


