package muni.fi.revrec.common.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.filePath.FilePathDAO;
import muni.fi.revrec.model.reviewer.Developer;
import muni.fi.revrec.model.reviewer.DeveloperDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class GerritPullRequestParser implements PullRequestParser {

    @Autowired
    private FilePathDAO filePathDAO;

    @Autowired
    private DeveloperDAO developerDAO;

    private JsonObject jsonObject;

    @Override
    public Set<FilePath> getFilePaths() {
        Set<FilePath> result = new HashSet<>();
        Map<String, Object> map = new HashMap<>();
        map = new Gson().fromJson(jsonObject.get("revisions").getAsJsonObject().get(jsonObject.get("current_revision").getAsString()).getAsJsonObject().get("files"), map.getClass());

        for (String file : map.keySet()) {
            FilePath filePath = new FilePath();
            filePath.setLocation(file);
            result.add(filePath);
        }

        return result;
    }

    @Override
    public String getChangeId() {
        return jsonObject.get("id").getAsString();
    }

    @Override
    public Integer getChangeNumber() {
        return jsonObject.get("_number").getAsInt();
    }

    @Override
    public Developer getOwner() {
        return parseDeveloper(jsonObject.get("owner"));
    }

    @Override
    public String getSubProject() {
        return jsonObject.get("project").getAsString();
    }

    @Override
    public Long getTimeStamp() {
        String created = jsonObject.get("created").getAsString();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return formatter.parse(created).getTime();
        } catch (ParseException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public Set<Developer> getReviewers() {
        Set<Developer> result = new HashSet<>();
        JsonArray reviewers = ((JsonArray) ((JsonObject) ((JsonObject) jsonObject.get("labels")).get("Code-Review")).get("all"));
        for (JsonElement jsonElement : reviewers) {
            int value = ((JsonObject) jsonElement).get("value").getAsInt();
            if (value > 0) {
                result.add(parseDeveloper(jsonElement));
            }
        }
        return result;
    }

    private Developer parseDeveloper(JsonElement jsonElement) {
        Developer developer = new Developer();
        developer.setAccountId(((JsonObject) jsonElement).get("_account_id").getAsString());
        developer.setName(((JsonObject) jsonElement).get("name").getAsString());
        developer.setEmail(((JsonObject) jsonElement).get("email").getAsString());
        //developer.setAvatar(((JsonObject) ((JsonObject) jsonElement).get("avatars").getAsJsonArray().get(0)).get("url").getAsString());
        return developer;
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    @Override
    public void setJsonObject(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }
}
