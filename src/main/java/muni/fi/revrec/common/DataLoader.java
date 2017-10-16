package muni.fi.revrec.common;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.filePath.FilePathDAO;
import muni.fi.revrec.model.project.ProjectDAO;
import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.pullRequest.PullRequestDAO;
import muni.fi.revrec.model.reviewer.ReviewerDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to load data from different sources and stores them in the database.
 *
 * @author Jakub Lipcak, Masaryk University
 */
@Service
public class DataLoader {

    private final Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private ProjectDAO projectDAO;

    @Autowired
    private FilePathDAO filePathDAO;

    @Autowired
    private PullRequestDAO pullRequestDAO;

    @Autowired
    private ReviewerDAO reviewerDAO;

    @Value("${gerrit.url}")
    private String gerritUrl;

    public void test() throws UnirestException {
        int start = 9893;
        int iteration = 0;
        int totalPullsInOneRequest = 100;
        int processedPullRequests = 0;
        try {
            do {
                HttpResponse<String> jsonResponse = Unirest.get(gerritUrl + "/changes/")
                        .queryString("start", String.valueOf((iteration * totalPullsInOneRequest) + start))
                        .queryString("n", totalPullsInOneRequest)
                        .queryString("q", "status:merged")
                        .queryString("o", "LABELS")
                        .queryString("o", "DETAILED_ACCOUNTS")
                        .queryString("o", "CURRENT_REVISION")
                        .queryString("o", "CURRENT_COMMIT")
                        .queryString("o", "CURRENT_FILES")
                        .asString();
                String json = jsonResponse.getBody().substring(5);

                for (int x = 0; x < totalPullsInOneRequest; x++) {
                    processedPullRequests = (iteration * totalPullsInOneRequest) + x + start;
                    JsonObject jsonObject = ((JsonArray) new JsonParser().parse(json)).get(x).getAsJsonObject();
                    System.out.println(processedPullRequests + " : " + jsonObject.get("id").getAsString());
                    PullRequest pullRequest = parse(jsonObject);
                }
                iteration++;
            } while (true);
        } catch (Exception ex) {
            logger.info("Processed pull requests: " + processedPullRequests);
            logger.error(ex);
        }

    }

    public PullRequest parse(JsonObject jsonObject) {
        Set<FilePath> filePaths = new HashSet<>();
        PullRequest pullRequest = new PullRequest();
        Map<String,Object> map = new HashMap<String,Object>();
        map = new Gson().fromJson(jsonObject.get("revisions").getAsJsonObject().get(jsonObject.get("current_revision").getAsString()).getAsJsonObject().get("files"), map.getClass());
        for(String file : map.keySet()){
            System.out.println(file);
        }
        return pullRequest;
    }
/*
https://git.eclipse.org/r/changes/?q=109889&o=CURRENT_REVISION&o=CURRENT_COMMIT&o=CURRENT_FILES
https://android-review.googlesource.com/changes/499912/?o=DETAILED_LABELS
 */
//    /**
//     * Extract data from JSON file and store them in the database. Only information about reviewers is fetched from Gerrit.
//     *
//     * @param fileLocation  location of JSON file
//     * @param gerritBrowser instance of Gerrit browser for chosen project
//     * @param projectName   name of the project
//     * @throws RestApiException
//     * @throws ParseException
//     */
//    public void loadDataFromFile(String fileLocation, GerritService gerritBrowser, String projectName) throws RestApiException, ParseException {
//        try (BufferedReader br = new BufferedReader(new FileReader(fileLocation))) {
//            StringBuilder sb = new StringBuilder();
//            String line = br.readLine();
//            int x = 0;
//
//            Project project = new Project();
//            if (projectDAO.findOne(projectName) != null) {
//                project = projectDAO.findOne(projectName);
//            } else {
//                project.setName(projectName);
//                project = projectDAO.save(project);
//            }
//
//            while (line != null) {
//                x++;
//                sb.append(line);
//                sb.append(System.lineSeparator());
//                line = br.readLine();
//
//                if (line == null) {
//                    return;
//                } else {
//                    System.out.println(x);
//                    JSONObject obj = new JSONObject(line);
//
//                    Set<Reviewer> reviewers = getSetOfReviewersFromJsonObject(obj, gerritBrowser);
//
//                    PullRequest pullRequest = new PullRequest();
//                    pullRequest.setChangeId(Integer.toString((Integer) obj.get("changeId")));
//                    pullRequest.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse((String) obj.get("submit_date")).getTime());
//
//                    pullRequest.setReviewers(reviewers);
//                    pullRequest.setSubProject(obj.getString("project"));
//                    pullRequest.setProject(project);
//                    pullRequest = pullRequestDAO.save(pullRequest);
//
//
//                    JSONArray arr = obj.getJSONArray("files");
//                    for (int i = 0; i < arr.length(); i++) {
//                        FilePath filePath = new FilePath();
//                        filePath.setLocation((String) arr.get(i));
//                        filePath.setPullRequest(pullRequest);
//                        filePathDAO.save(filePath);
//                    }
//                }
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Extract change IDs from JSON file and fetch all information about these change requests from Gerrit.
//     *
//     * @param fileLocation  location of JSON file
//     * @param gerritBrowser instance of Gerrit browser for chosen project
//     * @param projectName   name of the project
//     * @throws RestApiException
//     * @throws ParseException
//     */
//    public void loadDataByChangeIdsFromFile(String fileLocation, GerritService gerritBrowser, String projectName) throws RestApiException, ParseException {
//        try (BufferedReader br = new BufferedReader(new FileReader(fileLocation))) {
//            StringBuilder sb = new StringBuilder();
//            String line = br.readLine();
//            int x = 0;
//
//            while (line != null) {
//                x++;
//                sb.append(line);
//                sb.append(System.lineSeparator());
//                line = br.readLine();
//
//                if (line == null) {
//                    return;
//                } else {
//                    if (x >= 0) {
//                        try {
//                            System.out.println(x);
//                            JSONObject obj = new JSONObject(line);
//                            String changeId = Integer.toString((Integer) obj.get("changeId"));
//                            ChangeInfo changeInfo = gerritBrowser.getChange(changeId);
//                            List<String> paths = new ArrayList<>();
//                            JSONArray arr = ((JSONArray) obj.get("files"));
//                            for (int i = 0; i < arr.length(); i++) {
//                                paths.add(arr.getString(i));
//                            }
//                            Long timeCreated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse((String) obj.get("submit_date")).getTime();
//                            saveChangeRequest(changeInfo, projectName, gerritBrowser, getSetOfReviewersFromJsonObject(obj, gerritBrowser), paths, timeCreated);
//                        } catch (HttpStatusException | IndexOutOfBoundsException ex) {
//                            System.out.println(x);
//                            System.out.println(ex);
//                        }
//                    }
//                }
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Extract all previous pull requests from the information in GIT repository and store them in the database,
//     *
//     * @param gerritBrowser instance of Gerrit browser for chosen project
//     * @param gitBrowser    instance of Git browser for chosen project
//     * @param projectName   name of the project
//     * @throws GitAPIException
//     */
//    public void loadDataFromGit(GerritService gerritBrowser, GitService gitBrowser, String projectName) throws GitAPIException {
//        int counter = 0;
//        Git git = gitBrowser.getGit();
//
//        Iterable<RevCommit> commits = git.log().call();
//        List<RevCommit> commitsList = Lists.newArrayList(commits);
//
//        for (RevCommit commit : commitsList) {
//            try {
//                String changeId = getChangeIdFromFooter(commit.getFooterLines());
//                if (pullRequestDAO.findByChangeIdAndProjectName(changeId, projectName).size() > 0) {
//                    continue;
//                }
//
//                List<AccountInfo> accountInfos = getUserRelatedToCommit(commit, gerritBrowser);
//                System.out.println(counter + "   " + changeId + "   " + accountInfos.size());
//
//                if (accountInfos.isEmpty()) {
//                    continue;
//                }
//
//                ChangeInfo changeInfo = gerritBrowser.getChange(changeId);
//                saveChangeRequest(changeInfo, projectName, gerritBrowser);
//
//                counter++;
//            } catch (Exception ex) {
//                System.out.println(ex);
//            }
//        }
//    }
//
//    private void saveChangeRequest(ChangeInfo changeInfo, String projectName, GerritService gerritBrowser) throws RestApiException {
//        saveChangeRequest(changeInfo, projectName, gerritBrowser, new HashSet<>(), new ArrayList<>(), null);
//    }
//
//    private void saveChangeRequest(ChangeInfo changeInfo, String projectName, GerritService gerritBrowser, Set<Developer> specificCodeReviewers, List<String> paths, Long time) throws RestApiException {
//        Project project = new Project();
//        if (projectDAO.findOne(projectName) != null) {
//            project = projectDAO.findOne(projectName);
//        } else {
//            project.setName(projectName);
//            project = projectDAO.save(project);
//        }
//
//        /*if (changeInfo.reviewers == null) {
//            System.out.println("NULL");
//            return;
//        }*/
//
//        Set<Developer> allReviewers = new HashSet<>();
//        if (changeInfo.reviewers != null) {
//            allReviewers = new HashSet<>();
//            for (AccountInfo accountInfo : changeInfo.reviewers.values().iterator().next()) {
//                Developer reviewer = reviewerDAO.findOne(accountInfo._accountId);
//                if (reviewer == null) {
//                    reviewer = new Developer(accountInfo);
//                    reviewerDAO.save(reviewer);
//                }
//                allReviewers.add(reviewer);
//            }
//        }
//
//        Developer owner = reviewerDAO.findOne(changeInfo.owner._accountId);
//        if (owner == null) {
//            owner = new Developer(changeInfo.owner);
//            owner = reviewerDAO.save(owner);
//        }
//
//        PullRequest pullRequest = new PullRequest();
//        pullRequest.setChangeId(changeInfo.changeId);
//        if (time == null) {
//            pullRequest.setTime(changeInfo.created.getTime());
//        } else {
//            pullRequest.setTime(time);
//        }
//
//        pullRequest.setProject(project);
//        pullRequest.setChangeNumber(changeInfo._number);
//        pullRequest.setSubProject(changeInfo.project);
//        pullRequest.setOwner(owner);
//        pullRequest.setInsertions(changeInfo.insertions);
//        pullRequest.setDeletions(changeInfo.deletions);
//
//        /*pullRequest.setCodeReviewers(getSetOfReviewersInDbFromCollectionOfAccountInfos(gerritBrowser.getReviewers(changeInfo, "Code-Review")));
//        pullRequest.setVerifiedReviewers(getSetOfReviewersInDbFromCollectionOfAccountInfos(gerritBrowser.getReviewers(changeInfo, "Verified")));
//        pullRequest.setAllReviewers(allReviewers);
//        pullRequest.setAllCommentators(getSetOfReviewersInDbFromCollectionOfAccountInfos(gerritBrowser.getCommentators(changeInfo)));*/
//        pullRequest.setReviewers(specificCodeReviewers);
//
//        pullRequest = pullRequestDAO.save(pullRequest);
//
//        //List<String> paths = gerritBrowser.getFilePaths(changeInfo.changeId);
//        for (String path : paths) {
//            FilePath filePath = new FilePath(path, pullRequest);
//            filePathDAO.save(filePath);
//        }
//
//    }
//
//    //fetch, save and return all reviewers specified in JSONObject
//    private Set<Developer> getSetOfReviewersFromJsonObject(JSONObject obj, GerritService gerritBrowser) throws RestApiException {
//        JSONArray arr = obj.getJSONArray("approve_history");
//        Set<Developer> reviewers = new HashSet<>();
//        for (int i = 0; i < arr.length(); i++) {
//            Integer reviewerId = ((Integer) ((JSONObject) arr.get(i)).get("userId"));
//            Developer reviewer = reviewerDAO.findOne(reviewerId);
//            if (reviewer == null) {
//                AccountInfo accountInfo = gerritBrowser.getAccount(reviewerId.toString());
//                Developer newReviewer = new Developer();
//                newReviewer.setId(accountInfo._accountId);
//                newReviewer.setEmail(accountInfo.email);
//                newReviewer.setName(accountInfo.name);
//                if (accountInfo.avatars != null && accountInfo.avatars.size() > 0) {
//                    newReviewer.setAvatar(accountInfo.avatars.get(0).url);
//                }
//                reviewer = reviewerDAO.save(newReviewer);
//            }
//            reviewers.add(reviewer);
//        }
//        return reviewers;
//    }
//
//    //fetch, save and return all reviewers specified in Collection<AccountInfo>
//    private Set<Developer> getSetOfReviewersInDbFromCollectionOfAccountInfos(Collection<AccountInfo> accountInfos) {
//        Set<Developer> result = new HashSet<>();
//        for (AccountInfo accountInfo : accountInfos) {
//            if (accountInfo != null) {
//                Developer reviewer = reviewerDAO.findOne(accountInfo._accountId);
//                if (reviewer == null) {
//                    reviewer = reviewerDAO.save(new Developer(accountInfo));
//                }
//                result.add(reviewer);
//            }
//        }
//        return result;
//    }
//
//    private List<AccountInfo> getUserRelatedToCommit(RevCommit commit, GerritService gerritBrowser) throws RestApiException {
//        String changeId = getChangeIdFromFooter(commit.getFooterLines());
//        if (changeId.equals("")) {
//            return new ArrayList<>();
//        }
//        return (List<AccountInfo>) gerritBrowser.getReviewers(changeId, "Code-Review");
//    }
//
//    private String getChangeIdFromFooter(List<FooterLine> commitFooter) {
//        if (commitFooter.size() == 0) {
//            return "";
//        }
//        return commitFooter.get(0).getValue();
//    }

}

