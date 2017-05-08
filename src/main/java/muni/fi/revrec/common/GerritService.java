package muni.fi.revrec.common;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.*;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import muni.fi.revrec.common.exception.ReviewerRecommendationException;
import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.project.ProjectDAO;
import muni.fi.revrec.model.pullRequest.PullRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * This class implements communication with Gerrit system via REST API.
 *
 * @author Jakub Lipcak, Masaryk University
 */
@Service
public class GerritService {

    private GerritApi gerritApi;
    private final Log logger = LogFactory.getLog(this.getClass());

    public GerritService(@Autowired ProjectDAO projectDAO,
                         @Value("${recommendation.project}") String project) {
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritAuthData.Basic authData = new GerritAuthData.Basic(projectDAO.findOne(project).getGerritUrl());
        this.gerritApi = gerritRestApiFactory.create(authData);
    }

    public Collection<AccountInfo> getReviewers(String changeId, String label) throws RestApiException {
        ChangeInfo changeInfo = null;
        try {
            changeInfo = getChange(changeId);
        } catch (HttpStatusException ex) {
            logger.error(ex.getMessage());
            throw new ReviewerRecommendationException(ex.getMessage());
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

            return result;
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            logger.error(ex.getMessage());
            throw new ReviewerRecommendationException(ex.getMessage());
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
        } catch (NoSuchElementException | IllegalArgumentException | HttpStatusException ex) {
            throw new ReviewerRecommendationException(ex.getMessage());
        }
    }

    public AccountInfo getAccount(String id) throws RestApiException {
        return gerritApi.accounts().id(id).get();
    }

    public List<FilePath> getFilePaths(String changeId) throws RestApiException {
        List<FilePath> result = new ArrayList<>();
        Map<String, FileInfo> changeInfo = gerritApi.changes().id(changeId).revision("current").files();

        for (Map.Entry<String, FileInfo> entry : changeInfo.entrySet()) {
            if (!entry.getKey().equals("/COMMIT_MSG")) {
                result.add(new FilePath(entry.getKey()));
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

    public PullRequest getPullRequest(String gerritChangeNumber) {
        try {
            ChangeInfo changeInfo = getChange(gerritChangeNumber);
            PullRequest pullRequest = new PullRequest();
            pullRequest.setTimestamp(changeInfo.created.getTime());
            Set<FilePath> result = new HashSet<>(getFilePaths(gerritChangeNumber));
            pullRequest.setFilePaths(result);

            return pullRequest;
        } catch (RestApiException ex) {
            throw new ReviewerRecommendationException(ex.getMessage());
        }
    }
}
