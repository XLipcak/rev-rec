package muni.fi.revrec.common;

import com.google.gerrit.extensions.api.GerritApi;
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
import muni.fi.revrec.model.reviewer.Developer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * This class implements communication with Gerrit system via its REST API.
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

    /**
     * Get accounts, who labeled the pull request with specified changeId or owned it.
     *
     * @param changeId Gerrit changeId of the pull request.
     * @param label    label value.
     * @return list of accounts, who labeled the given pull request with the specified label or owned it.
     * @throws RestApiException if there is a problem with the communication via Gerrit REST API.
     */
    public Collection<AccountInfo> getReviewersAndOwnersOfCommit(String changeId, String label) throws RestApiException {
        ChangeInfo changeInfo = null;
        try {
            changeInfo = getChange(changeId);
        } catch (HttpStatusException ex) {
            logger.error(ex.getMessage());
            throw new ReviewerRecommendationException(ex.getMessage());
        }
        return getReviewersAndOwnersOfCommit(changeInfo, label);
    }

    /**
     * Get accounts, who labeled the given changeInfo or owned it.
     *
     * @param changeInfo changeInfo representing the Gerrit pull request.
     * @param label      label value.
     * @return list of accounts, who labeled the given pull request with the specified label or owned it.
     * @throws RestApiException if there is a problem with the communication via Gerrit REST API.
     */
    public Collection<AccountInfo> getReviewersAndOwnersOfCommit(ChangeInfo changeInfo, String label) throws RestApiException {
        if (changeInfo == null) {
            return new ArrayList<>();
        }
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
            result.add(changeInfo.owner);
            return result;
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            logger.error(ex.getMessage());
            throw new ReviewerRecommendationException(ex.getMessage());
        }
    }

    /**
     * Get accounts, which commented the pull request given as a parameter.
     *
     * @param changeInfo changeInfo representing the Gerrit pull request.
     * @return list of accounts, which commented the pull request.
     * @throws RestApiException if there is a problem with the communication via Gerrit REST API.
     */
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


    /**
     * Get account info with specified Gerrit id.
     *
     * @param id id of an account.
     * @return account with specified Gerrit id.
     * @throws RestApiException if there is a problem with the communication via Gerrit REST API.
     */
    public AccountInfo getAccount(String id) throws RestApiException {
        return gerritApi.accounts().id(id).get();
    }

    /**
     * Get file paths modified in the pull request with Gerrit changeId.
     *
     * @param changeId change id of the Gerrit pull request.
     * @return file paths modified in the pull request.
     * @throws RestApiException if there is a problem with the communication via Gerrit REST API.
     */
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

    /**
     * Get change info from change id.
     *
     * @param changeId Gerrit change id.
     * @return object of class ChangeInfo containing information about pull request with changeId.
     * @throws RestApiException if there is a problem with the communication via Gerrit REST API.
     */
    public ChangeInfo getChange(String changeId) throws RestApiException {
        //dependent on Gerrit instance
        //ChangeInfo changeInfo = gerritApi.changes().id(changeId).get();

        List<ChangeInfo> changeInfos = gerritApi.changes().query(changeId).withOption(ListChangesOption.DETAILED_ACCOUNTS).withOption(ListChangesOption.DETAILED_LABELS).get();
        if (changeInfos.size() > 0) {
            return changeInfos.get(0);
        } else {
            return null;
        }
    }

    /**
     * Get pull request from gerritChangeNumber.
     *
     * @param gerritChangeNumber change number of the pull request in Gerrit.
     * @return pullRequest object containing timestamp, changeId, file paths, sub-project name
     * and owner information of the pull request with gerritChangeNumber.
     */
    public PullRequest getPullRequest(String gerritChangeNumber) {
        try {
            ChangeInfo changeInfo = getChange(gerritChangeNumber);
            Set<FilePath> result = new HashSet<>(getFilePaths(gerritChangeNumber));

            PullRequest pullRequest = new PullRequest();
            pullRequest.setTimestamp(changeInfo.created.getTime());
            pullRequest.setFilePaths(result);
            pullRequest.setSubProject(changeInfo.project);
            pullRequest.setOwner(new Developer(changeInfo.owner));
            pullRequest.setChangeId(changeInfo.changeId);

            return pullRequest;
        } catch (RestApiException ex) {
            throw new ReviewerRecommendationException(ex.getMessage());
        }
    }
}
