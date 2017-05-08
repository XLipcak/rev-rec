package muni.fi.revrec.recommendation.reviewbot;

import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import muni.fi.revrec.common.GerritService;
import muni.fi.revrec.common.GitService;
import muni.fi.revrec.common.exception.ReviewerRecommendationException;
import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.pullRequest.PullRequestDAO;
import muni.fi.revrec.model.reviewer.Developer;
import muni.fi.revrec.recommendation.ReviewerRecommendation;
import muni.fi.revrec.recommendation.ReviewerRecommendationBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.revwalk.FooterLine;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Implementation of ReviewBot algorithm: https://labs.vmware.com/download/198/
 *
 * @author Jakub Lipcak, Masaryk University
 */
@Service
public class ReviewBot extends ReviewerRecommendationBase implements ReviewerRecommendation {

    private static final int INITIAL_POINT = 1;
    private static final double CONSTANT_FACTOR = 0.9;

    private GitService gitService;
    private GerritService gerritService;

    private final Log logger = LogFactory.getLog(this.getClass());


    public ReviewBot(@Autowired PullRequestDAO pullRequestDAO,
                     @Autowired GitService gitService,
                     @Autowired GerritService gerritService,
                     @Value("${recommendation.retired}") boolean removeRetiredReviewers,
                     @Value("${recommendation.retired.interval}") long timeRetiredInMonths,
                     @Value("${recommendation.project}") String project) {
        super(pullRequestDAO, removeRetiredReviewers, timeRetiredInMonths, project);
        this.gitService = gitService;
        this.gerritService = gerritService;
    }

    @Override
    public void buildModel() {
    }

    @Override
    public List<Developer> recommend(PullRequest pullRequest) {

        Map<RevCommit, Double> resultMap = new HashMap<>();
        for (FilePath filePath : pullRequest.getFilePaths()) {
            List<CommitAndPathWrapper> fileCommitHistory = gitService.getFileCommitHistory(filePath.getLocation(), pullRequest.getSubProject());
            if (fileCommitHistory.size() == 1) {
                continue;
            }
            Set<Integer> lines = getLinesAffectedByCommit(fileCommitHistory.get(0).getRevCommit(), fileCommitHistory.get(1).getRevCommit(), filePath.getLocation(), pullRequest.getSubProject());
            List<List<RevCommit>> lch = lineChangeHistory(filePath.getLocation(), lines, fileCommitHistory, pullRequest.getSubProject());
            for (int x = 0; x < lch.size(); x++) {
                double points = getInitialPointForThisFile(filePath.getLocation());
                for (int y = 0; y < lch.get(x).size(); y++) {
                    RevCommit entry = lch.get(x).get(y);
                    if (resultMap.containsKey(entry)) {
                        resultMap.replace(entry, resultMap.get(entry) + points);
                    } else {
                        resultMap.put(entry, points);
                    }
                    points *= CONSTANT_FACTOR;
                }
            }
        }

        return processResult(propagateResultToUserPoints(resultMap), pullRequest);
    }


    private Map<Developer, Double> propagateResultToUserPoints(Map<RevCommit, Double> pointsMap) {
        Map<Developer, Double> result = new HashMap<>();

        Map<AccountInfo, Double> reviewerCandidates = new HashMap<>();
        Set<String> emails = new HashSet<>();

        try {
            Iterator it = pointsMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                for (AccountInfo user : getUserRelatedToCommit((RevCommit) pair.getKey())) {
                    if (reviewerCandidates.containsKey(user)) {
                        reviewerCandidates.replace(user, reviewerCandidates.get(user) + (Double) pair.getValue());
                    } else {
                        reviewerCandidates.put(user, (Double) pair.getValue());
                        emails.add(user.email);
                    }
                }
                it.remove();
            }

            //normalisation, to refactor later
            for (String email : emails) {
                double points = 0;
                AccountInfo accountInfo = null;
                for (Map.Entry<AccountInfo, Double> entry : reviewerCandidates.entrySet()) {
                    if (entry.getKey().email.equals(email)) {
                        points += entry.getValue();
                        accountInfo = entry.getKey();
                    }
                }
                result.put(new Developer(accountInfo.email, accountInfo.name), points);
            }
        } catch (RestApiException ex) {
            logger.error(ex.getMessage());
            throw new ReviewerRecommendationException(ex.getMessage());
        }
        return result;
    }

    private List<AccountInfo> getUserRelatedToCommit(RevCommit commit) throws RestApiException {
        String changeId = getChangeIdFromFooter(commit.getFooterLines());
        if (changeId.equals("")) {
            return new ArrayList<>();
        }
        return (List<AccountInfo>) gerritService.getReviewers(changeId, "Code-Review");
    }

    private int getInitialPointForThisFile(String filePath) {
        return INITIAL_POINT;
    }

    private List<List<RevCommit>> lineChangeHistory(String filePath, Set<Integer> lines, List<CommitAndPathWrapper> fileCommitHistory, String subprojectName) {
        List<List<RevCommit>> result = new ArrayList<>();

        try {
            Integer[] linesArray = lines.toArray(new Integer[lines.size()]);
            int[][] lineHistoryMatrix = generateLineHistoryMatrix(filePath, linesArray, fileCommitHistory, subprojectName);
            boolean[][] trackingMatrix = generateTrackingMatrix(filePath, linesArray, fileCommitHistory, lineHistoryMatrix, subprojectName);
            List<Set<Integer>> alreadyCheckedLines = new ArrayList<>();
            for (int x = 0; x < fileCommitHistory.size(); x++) {
                alreadyCheckedLines.add(new HashSet<>());
            }

            logger.debug("File change history: ");
            fileCommitHistory.forEach(x -> logger.debug(getChangeIdFromFooter(x.getRevCommit().getFooterLines())));

            for (int index = 0; index < lines.size(); index++) {
                List<RevCommit> resultForActualLine = new ArrayList<>();
                for (int x = 1; x < fileCommitHistory.size(); x++) {
                    if (trackingMatrix[index][x]) {
                        continue;
                    }
                    if (x == fileCommitHistory.size() - 1) {
                        resultForActualLine.add(fileCommitHistory.get(x).getRevCommit());
                        logger.debug("Line " + linesArray[index] + " was initialized in " + fileCommitHistory.get(x).getRevCommit().getShortMessage());
                        break;
                    }
                    if (!fileCommitHistory.get(x).getPath().equals(fileCommitHistory.get(x).getPath())) {
                        x++;
                        continue;
                    }

                    int actualLine = linesArray[index] + lineHistoryMatrix[index][x + 1];

                    RevCommit headCommit = fileCommitHistory.get(x).getRevCommit();
                    RevCommit diffWith = fileCommitHistory.get(x + 1).getRevCommit();
                    EditList edits = gitService.diff(headCommit, diffWith, fileCommitHistory.get(x).getPath(), subprojectName);

                    for (Edit edit : edits) {
                        if (edit.getType() == Edit.Type.REPLACE) {
                            if (edit.getBeginA() < actualLine && actualLine <= edit.getEndA()) {
                                resultForActualLine.add(headCommit);
                                logger.debug("Line " + linesArray[index] + " was replaced in " + fileCommitHistory.get(x).getRevCommit().getShortMessage());
                            }
                        }

                        if (edit.getType() == Edit.Type.INSERT) {
                            if (linesArray[index] + lineHistoryMatrix[index][x] >= edit.getBeginB() + 1 && linesArray[index] + lineHistoryMatrix[index][x] <= edit.getEndB()) {
                                alreadyCheckedLines.get(x).add(actualLine);
                                resultForActualLine.add(headCommit);
                                logger.debug("Line " + linesArray[index] + " was inserted in " + fileCommitHistory.get(x).getRevCommit().getShortMessage());
                            }
                        }

                        if (edit.getType() == Edit.Type.DELETE) {
                            if (edit.getBeginA() < actualLine && actualLine <= edit.getEndA()) {
                                logger.debug("Line " + linesArray[index] + " was deleted in " + fileCommitHistory.get(x).getRevCommit().getShortMessage());
                            }
                        }
                    }
                }
                result.add(resultForActualLine);
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            throw new ReviewerRecommendationException(ex.getMessage());
        }

        return result;
    }

    private int[][] generateLineHistoryMatrix(String filePath, Integer[] linesArray, List<CommitAndPathWrapper> fileCommitHistory, String subProjectName) throws IOException {
        int[][] lineHistoryMatrix = new int[linesArray.length][fileCommitHistory.size()];
        for (int y = 0; y < fileCommitHistory.size(); y++) {
            if (y == fileCommitHistory.size() - 1) {
                break;
            }
            if (!fileCommitHistory.get(y).getPath().equals(fileCommitHistory.get(y + 1).getPath())) {
                y++;
                continue;
            }

            RevCommit headCommit = fileCommitHistory.get(y).getRevCommit();
            RevCommit diffWith = fileCommitHistory.get(y + 1).getRevCommit();
            EditList edits = gitService.diff(headCommit, diffWith, fileCommitHistory.get(y).getPath(), subProjectName);
            for (Edit edit : edits) {
                if (edit.getType() == Edit.Type.INSERT) {
                    int amountOfInsertedLines = edit.getEndB() - edit.getBeginB();
                    for (int lines = 0; lines < linesArray.length; lines++) {
                        if (linesArray[lines] + lineHistoryMatrix[lines][y + 1] > edit.getBeginB() + 1) {
                            lineHistoryMatrix[lines][y + 1] -= amountOfInsertedLines;
                        }
                    }
                }

                if (edit.getType() == Edit.Type.DELETE) {
                    int amountOfDeletedLines = edit.getEndA() - edit.getBeginA();
                    for (int lines = 0; lines < linesArray.length; lines++) {
                        if (linesArray[lines] + lineHistoryMatrix[lines][y] > edit.getEndB() + 1) {
                            lineHistoryMatrix[lines][y + 1] += amountOfDeletedLines;
                        }
                    }
                }

                if (edit.getType() == Edit.Type.REPLACE) {
                    int lineDifferenceInReplace = (edit.getEndA() - edit.getBeginA()) - (edit.getEndB() - edit.getBeginB());
                    for (int lines = 0; lines < linesArray.length; lines++) {
                        if (linesArray[lines] + lineHistoryMatrix[lines][y + 1] + lineDifferenceInReplace > edit.getEndA()) {
                            lineHistoryMatrix[lines][y + 1] += lineDifferenceInReplace;
                        }
                    }
                }
            }
        }

        //transform
        for (int x = 0; x < lineHistoryMatrix.length; x++) {
            for (int y = +1; y < lineHistoryMatrix[0].length; y++) {
                lineHistoryMatrix[x][y] += lineHistoryMatrix[x][y - 1];
            }
        }
        return lineHistoryMatrix;
    }

    private boolean[][] generateTrackingMatrix(String filePath, Integer[] linesArray, List<CommitAndPathWrapper> fileCommitHistory, int[][] lineHistoryMatrix, String subProjectName) throws IOException {
        boolean[][] trackingMatrix = new boolean[linesArray.length][fileCommitHistory.size()];
        for (int y = 0; y < fileCommitHistory.size(); y++) {
            if (y == fileCommitHistory.size() - 1) {
                break;
            }
            if (!fileCommitHistory.get(y).getPath().equals(fileCommitHistory.get(y + 1).getPath())) {
                y++;
                continue;
            }

            RevCommit headCommit = fileCommitHistory.get(y).getRevCommit();
            RevCommit diffWith = fileCommitHistory.get(y + 1).getRevCommit();

            EditList edits = gitService.diff(headCommit, diffWith, fileCommitHistory.get(y).getPath(), subProjectName);
            for (Edit edit : edits) {

                if (edit.getType() == Edit.Type.INSERT) {
                    for (int x = edit.getBeginB(); x < edit.getEndB(); x++) {
                        for (int lines = 0; lines < linesArray.length; lines++) {
                            if (linesArray[lines] + lineHistoryMatrix[lines][y] == x + 1) {
                                for (int z = y + 1; z < fileCommitHistory.size(); z++)
                                    trackingMatrix[lines][z] = true;
                            }
                        }
                    }
                }

                if (edit.getType() == Edit.Type.REPLACE) {
                    int lineDifferenceInReplace = (edit.getEndA() - edit.getBeginA()) - (edit.getEndB() - edit.getBeginB());
                    if (lineDifferenceInReplace < 0) {
                        for (int lines = 0; lines < linesArray.length; lines++) {
                            int actualLine = linesArray[lines] + lineHistoryMatrix[lines][y + 1];
                            if (edit.getBeginA() < actualLine && actualLine <= edit.getBeginA() + (edit.getEndB() - edit.getBeginB()) && (actualLine - edit.getBeginA()) > (edit.getEndA() - edit.getBeginA())
                                    && linesArray[lines] + lineHistoryMatrix[lines][y] > edit.getBeginB() && linesArray[lines] + lineHistoryMatrix[lines][y] <= edit.getEndB()) {
                                for (int z = y; z < fileCommitHistory.size(); z++)
                                    trackingMatrix[lines][z] = true;
                            }
                        }
                    }
                }
            }
        }
        return trackingMatrix;
    }

    private Set<Integer> getLinesAffectedByCommit(RevCommit headCommit, RevCommit diffWith, String filePath, String subProjectName) {
        Set<Integer> result = new LinkedHashSet<>();
        try {
            EditList edits = gitService.diff(headCommit, diffWith, filePath, subProjectName);

            for (Edit edit : edits) {
                if (edit.getType() == Edit.Type.INSERT) {
                    result.add(gitService.getNearestLineOfCode(headCommit, filePath, edit.getBeginB() + 1, subProjectName));
                }
                if (edit.getType() == Edit.Type.DELETE) {
                    for (int x = edit.getBeginA() + 1; x <= edit.getEndA(); x++) {
                        result.add(x);
                    }
                }
                if (edit.getType() == Edit.Type.REPLACE) {
                    for (int x = edit.getBeginB() + 1; x <= edit.getEndB(); x++) {
                        result.add(x);
                    }
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            throw new ReviewerRecommendationException(ex.getMessage());
        }
        return result;
    }

    private String getChangeIdFromFooter(List<FooterLine> commitFooter) {
        if (commitFooter.size() == 0) {
            return "";
        }
        return commitFooter.get(0).getValue();
    }
}
