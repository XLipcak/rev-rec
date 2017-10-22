package muni.fi.revrec.common.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import muni.fi.revrec.common.exception.ReviewerRecommendationException;
import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.filePath.FilePathDAO;
import muni.fi.revrec.model.project.Project;
import muni.fi.revrec.model.project.ProjectDAO;
import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.pullRequest.PullRequestDAO;
import muni.fi.revrec.model.reviewer.Developer;
import muni.fi.revrec.model.reviewer.DeveloperDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
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
    private GerritPullRequestParser gerritPullRequestParser;

    @Autowired
    private GitHubPullRequestParser gitHubPullRequestParser;

    @Autowired
    private ProjectDAO projectDAO;

    @Autowired
    private FilePathDAO filePathDAO;

    @Autowired
    private PullRequestDAO pullRequestDAO;

    @Autowired
    private DeveloperDAO developerDAO;

    @Value("${recommendation.project}")
    private String projectName;

    @Value("${project.url}")
    private String projectUrl;

    public void fetchData() throws UnirestException {
        Project project = createIfNotExist(projectName);
        boolean isGit = projectUrl.contains("github");

        int start = 0;
        int iteration = 0;
        int totalPullsInOneRequest = 100;
        int processedPullRequests = 0;
        try {
            do {
                String json = "";
                PullRequestParser pullRequestParser;

                if (isGit) {
                    HttpResponse<String> jsonResponse = Unirest.get(projectUrl + "/pulls")
                            .queryString("state", "closed")
                            .queryString("page", iteration + 1)
                            .asString();
                    json = jsonResponse.getBody();
                    pullRequestParser = gitHubPullRequestParser;
                } else {
                    HttpResponse<String> jsonResponse = Unirest.get(projectUrl + "/changes/")
                            .queryString("start", String.valueOf((iteration * totalPullsInOneRequest) + start))
                            .queryString("n", totalPullsInOneRequest)
                            .queryString("q", "status:merged")
                            .queryString("o", "DETAILED_LABELS")
                            .queryString("o", "DETAILED_ACCOUNTS")
                            .queryString("o", "CURRENT_REVISION")
                            .queryString("o", "CURRENT_COMMIT")
                            .queryString("o", "CURRENT_FILES")
                            .asString();
                    json = jsonResponse.getBody().substring(5);
                    pullRequestParser = gerritPullRequestParser;
                }

                for (int x = 0; x < (isGit ? 30 : totalPullsInOneRequest); x++) {
                    System.out.println(x);

                    processedPullRequests = (iteration * totalPullsInOneRequest) + x + start;
                    JsonObject jsonObject = null;
                    if (isGit) {
                        jsonObject = new JsonParser().parse(getGithubPullRequestDetail(
                                ((JsonObject) ((JsonArray) new JsonParser().parse(json)).get(x)).get("number").getAsString())).getAsJsonObject();
                        if (jsonObject.get("merged_by").isJsonNull()) {
                            System.out.println("not merged");
                            continue;
                        }
                    } else {
                        jsonObject = ((JsonArray) new JsonParser().parse(json)).get(x).getAsJsonObject();
                    }

                    System.out.println(processedPullRequests);

                    pullRequestParser.setJsonObject(jsonObject);
                    process(jsonObject, pullRequestParser, project);
                }
                iteration++;

            } while (true);
        } catch (ReviewerRecommendationException ex) {
            logger.info("Processed pull requests: " + processedPullRequests);
            logger.error(ex.getMessage());
        }


    }

    private PullRequest process(JsonObject jsonObject, PullRequestParser pullRequestParser, Project project) {
        PullRequest pr = pullRequestDAO.findByProjectNameAndChangeId(projectName, pullRequestParser.getChangeId());
        PullRequest pullRequest = new PullRequest();

        try {
            if (pr == null) {
                pullRequest.setChangeId(pullRequestParser.getChangeId());
                pullRequest.setChangeNumber(pullRequestParser.getChangeNumber());
                pullRequest.setSubProject(pullRequestParser.getSubProject());
                pullRequest.setTimestamp(pullRequestParser.getTimeStamp());
                pullRequest.setOwner(createIfNotExist(pullRequestParser.getOwner()));
                pullRequest.setProject(project);
                pullRequest.setReviewers(createIfNotExist(pullRequestParser.getReviewers()));
            } else {
                return null;
            }
        } catch (Exception ex) {
            logger.error("Parsing exception: " + ex.getMessage());
            return null;
        }

        pullRequest = pullRequestDAO.save(pullRequest);
        create(pullRequestParser.getFilePaths(), pullRequest);
        return pullRequest;
    }


    private Developer createIfNotExist(Developer developer) {
        Developer dev = developerDAO.findByAccountIdAndNameAndEmail(developer.getAccountId(), developer.getName(), developer.getEmail());
        if (dev == null) {
            return developerDAO.save(developer);
        }
        return dev;
    }

    private Set<Developer> createIfNotExist(Set<Developer> reviewers) {
        Set<Developer> result = new HashSet<>();
        for (Developer reviewer : reviewers) {
            Developer developer = developerDAO.findByAccountIdAndNameAndEmail(reviewer.getAccountId(), reviewer.getName(), reviewer.getEmail());
            if (developer == null) {
                result.add(developerDAO.save(reviewer));
            } else {
                result.add(developer);
            }
        }
        return result;
    }

    private Project createIfNotExist(String projectName) {
        Project p = projectDAO.findOne(projectName);
        if (p == null) {
            return projectDAO.save(new Project(projectName, projectUrl));
        }
        return p;
    }

    private Set<FilePath> create(Set<FilePath> filePaths, PullRequest pullRequest) {
        Set<FilePath> result = new HashSet<>();
        for (FilePath filePath : filePaths) {
            filePath.setPullRequest(pullRequest);
            result.add(filePathDAO.save(filePath));
        }
        return result;
    }

    private String getGithubPullRequestDetail(String number) throws UnirestException {
        HttpResponse<String> jsonResponse = Unirest.get(projectUrl + "/pulls/" + number)
                .asString();
        return jsonResponse.getBody();
    }
}

