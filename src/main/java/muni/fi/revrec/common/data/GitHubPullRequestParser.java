package muni.fi.revrec.common.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.reviewer.Developer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class GitHubPullRequestParser implements PullRequestParser {

    @Value("${project.url}")
    private String projectUrl;

    private String gitHubToken;

    private JsonObject jsonObject;

    @Override
    public Set<FilePath> getFilePaths() {
        int changedFiles = jsonObject.get("changed_files").getAsInt();

        Set<FilePath> result = new HashSet<>();
        HttpResponse<String> jsonResponse = null;
        for (int x = 1; true; x++) {
            try {
                jsonResponse = Unirest.get(projectUrl + "/pulls/" + getChangeNumber() + "/files")
                        .queryString("access_token", gitHubToken)
                        .queryString("page", x)
                        .asString();
            } catch (UnirestException e) {
                throw new RuntimeException();
            }
            String json = jsonResponse.getBody();
            if(json.equals("[]")){
                return result;
            }

            if(json.equals("{\"message\":\"Not Found\",\"documentation_url\":\"https://developer.github.com/v3/pulls/#list-pull-requests-files\"}")){
                return null;
            }

            for (JsonElement jsonElement : ((JsonArray) new JsonParser().parse(json))) {
                FilePath filePath = new FilePath();
                filePath.setLocation(((JsonObject) jsonElement).get("filename").getAsString());
                result.add(filePath);
            }
        }
    }

    @Override
    public String getChangeId() {
        return jsonObject.get("id").getAsString();
    }

    @Override
    public Integer getChangeNumber() {
        return jsonObject.get("number").getAsInt();
    }

    @Override
    public Developer getOwner() {
        return parseDeveloper(jsonObject.get("user"));
    }

    @Override
    public String getSubProject() {
        return "";
    }

    @Override
    public Long getTimeStamp() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).parse(jsonObject.get("created_at").getAsString()).getTime();
        } catch (ParseException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public Set<Developer> getReviewers() {
        Set<Developer> result = new HashSet<>();
        int changeNumber = getChangeNumber();
        //result.add(parseDeveloper(jsonObject.get("merged_by")));

        HttpResponse<String> jsonResponse;
        String json;

        try {
            jsonResponse = Unirest.get(projectUrl + "/issues/" + getChangeNumber() + "/comments")
                    .queryString("access_token", gitHubToken)
                    .asString();
        } catch (UnirestException e) {
            throw new RuntimeException();
        }

        json = jsonResponse.getBody();

        for (JsonElement jsonElement : ((JsonArray) new JsonParser().parse(json))) {
            Developer developer = parseDeveloper(jsonElement.getAsJsonObject().get("user"));
            if (!developer.getName().contains("bot")) {
                result.add(developer);
            }
        }

        try {
            jsonResponse = Unirest.get(projectUrl + "/pulls/" + getChangeNumber() + "/reviews")
                    .queryString("access_token", gitHubToken)
                    .asString();
        } catch (UnirestException e) {
            throw new RuntimeException();
        }

        json = jsonResponse.getBody();

        if(json.equals("{\"message\":\"Not Found\",\"documentation_url\":\"https://developer.github.com/v3\"}")){
            return result;
        }

        for (JsonElement jsonElement : ((JsonArray) new JsonParser().parse(json))) {
            Developer developer = parseDeveloper(jsonElement.getAsJsonObject().get("user"));
            if (!developer.getName().contains("bot")) {
                result.add(developer);
            }
        }

        return result;
    }

    @Override
    public void setJsonObject(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    private Developer parseDeveloper(JsonElement jsonElement) {
        Developer developer = new Developer();
        developer.setAccountId(((JsonObject) jsonElement).get("id").getAsString());
        developer.setName(((JsonObject) jsonElement).get("login").getAsString());
        developer.setEmail("");
        developer.setAvatar(((JsonObject) jsonElement).get("avatar_url").getAsString());
        return developer;
    }

    public String getGitHubToken() {
        return gitHubToken;
    }

    public void setGitHubToken(String gitHubToken) {
        this.gitHubToken = gitHubToken;
    }
}
