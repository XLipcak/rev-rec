package muni.fi.reviewrecommendations.common;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.*;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;

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

    public Collection<AccountInfo> getReviewers(String changeId, String label) throws RestApiException {
        ChangeInfo changeInfo = null;
        try {
            changeInfo = getChange(changeId);
        } catch (HttpStatusException ex) {
            return new ArrayList<>();
        }
        return getReviewers(changeInfo, label);
    }

    public Collection<AccountInfo> getReviewers(ChangeInfo changeInfo, String label) throws RestApiException {
        try {
            List<AccountInfo> result = new ArrayList<>();

            LabelInfo labelInfo = changeInfo.labels.get(label);
            if (labelInfo.all == null) {
                return result;
            }
            for (ApprovalInfo approvalInfo : labelInfo.all) {
                if (approvalInfo.value != null && approvalInfo.value > 0) {
                    AccountInfo accountInfo = new AccountInfo(approvalInfo._accountId);
                    accountInfo.email = approvalInfo.email;
                    accountInfo.name = approvalInfo.name;
                    accountInfo.avatars = approvalInfo.avatars;
                    result.add(accountInfo);
                }
            }

            return result;//changeInfo.reviewers.values().iterator().next();
        } catch (NoSuchElementException ex) {
            return new ArrayList<>();
        } catch (IllegalArgumentException ex) {
            return new ArrayList<>();
        }
    }

    public Collection<AccountInfo> getCommentators(ChangeInfo changeInfo) throws RestApiException {
        List<AccountInfo> result = new ArrayList<>();

        if (changeInfo.messages == null) {
            return result;
        }
        for (ChangeMessageInfo changeMessageInfo : changeInfo.messages) {
            result.add(changeMessageInfo.author);
        }
        return result;
    }

    public int getChangeNumber(String changeId) throws RestApiException {
        try {
            ChangeInfo changeInfo = gerritApi.changes().id(changeId).get();
            return changeInfo._number;
        } catch (NoSuchElementException ex) {
            return -1;
        } catch (IllegalArgumentException ex) {
            return -1;
        } catch (HttpStatusException ex) {
            return -1;
        }
    }

    public AccountInfo getAccount(String id) throws RestApiException {
        return gerritApi.accounts().id(id).get();
    }

    public List<String> getFilePaths(String changeId) throws RestApiException {
        List<String> result = new ArrayList<>();
        Map<String, FileInfo> changeInfo = gerritApi.changes().id(changeId).revision("current").files();

        for (Map.Entry<String, FileInfo> entry : changeInfo.entrySet()) {
            if (!entry.getKey().equals("/COMMIT_MSG")) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public ChangeInfo getChange(String changeId) throws RestApiException {
        //dependent on Gerrit instance
        //ChangeInfo changeInfo = gerritApi.changes().id(changeId).get();
        ChangeInfo changeInfo = gerritApi.changes().query(changeId).withOption(ListChangesOption.DETAILED_ACCOUNTS).get().get(0);
        return changeInfo;
    }

    public List<ChangeInfo> getGerritChanges(int start) throws RestApiException {
        Changes.QueryRequest queryRequest = gerritApi.changes().query();
        queryRequest = queryRequest.withStart(start);
        return queryRequest.get();
    }
}
