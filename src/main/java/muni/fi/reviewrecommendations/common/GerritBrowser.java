package muni.fi.reviewrecommendations.common;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;

import java.util.*;

/**
 * @author Jakub Lipcak, Masaryk University
 */
public class GerritBrowser {
    private String gerritPath;
    private GerritApi gerritApi;

    public GerritBrowser(String gerritPath) {
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritAuthData.Basic authData = new GerritAuthData.Basic(gerritPath);
        this.gerritApi = gerritRestApiFactory.create(authData);
    }

    public Collection<AccountInfo> getReviewers(String changeId) throws RestApiException {
        try{
            ChangeInfo changeInfo = gerritApi.changes().id(changeId).get();
            return changeInfo.reviewers.values().iterator().next();
        }catch (NoSuchElementException ex){
            return new ArrayList<>();
        }
    }

    public List<String> getFilePaths(String changeId) throws RestApiException {
        List<String> result = new ArrayList<>();
        Map<String, FileInfo> changeInfo = gerritApi.changes().id(changeId).revision("current").files();

        for (Map.Entry<String, FileInfo> entry : changeInfo.entrySet()) {
            if(!entry.getKey().equals("/COMMIT_MSG")){
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
