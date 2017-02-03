package muni.fi.reviewrecommendations.recommendationTechniques.reviewbot;

import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import muni.fi.reviewrecommendations.common.GerritBrowser;
import muni.fi.reviewrecommendations.common.GitBrowser;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import muni.fi.reviewrecommendations.recommendationTechniques.ReviewerRecommendation;
import muni.fi.reviewrecommendations.recommendationTechniques.Review;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FooterLine;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.RawParseUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ReviewBot: https://labs.vmware.com/download/198/
 *
 * @author Jakub Lipcak, Masaryk University
 */
public class ReviewBot implements ReviewerRecommendation {

    private GitBrowser gitBrowser;
    private GerritBrowser gerritBrowser;
    private static final int INITIAL_POINT = 1;
    private static final double CONSTANT_FACTOR = 0.9;

    public ReviewBot(GitBrowser gitBrowser, GerritBrowser gerritBrowser) {
        this.gitBrowser = gitBrowser;
        this.gerritBrowser = gerritBrowser;
    }

    @Override
    public Map<Reviewer, Double> reviewersRankingAlgorithm(Review review) {

        Map<RevCommit, Double> resultMap = new HashMap<>();
        for (String filePath : review.getFilePaths()) {
            List<CommitAndPathWrapper> fileCommitHistory = gitBrowser.getFileCommitHistory(filePath);
            if (fileCommitHistory.size() == 1) {
                continue;
            }
            Set<Integer> lines = getLinesAffectedByCommit(fileCommitHistory.get(0).getRevCommit(), fileCommitHistory.get(1).getRevCommit(), filePath);
            List<List<RevCommit>> lch = lineChangeHistory(filePath, lines, fileCommitHistory);
            for (int x = 0; x < lch.size(); x++) {
                double points = getInitialPointForThisFile(filePath);
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

        return propagateResultToUserPoints(resultMap);
    }


    private Map<Reviewer, Double> propagateResultToUserPoints(Map<RevCommit, Double> pointsMap) {
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
            Map<Reviewer, Double> result = new HashMap<>();
            for (String email : emails) {
                double points = 0;
                AccountInfo accountInfo = null;
                for (Map.Entry<AccountInfo, Double> entry : reviewerCandidates.entrySet()) {
                    if (entry.getKey().email.equals(email)) {
                        points += entry.getValue();
                        accountInfo = entry.getKey();
                    }
                }
                result.put(new Reviewer(accountInfo.email, accountInfo.name), points);
            }
            return sortByValue(result);
        } catch (RestApiException ex) {
            //TODO: handle exception
        }
        return new HashMap<>();
    }

    private List<AccountInfo> getUserRelatedToCommit(RevCommit commit) throws RestApiException {
        String changeId = getChangeIdFromFooter(commit.getFooterLines());
        if (changeId.equals("")) {
            return new ArrayList<>();
        }
        return (List<AccountInfo>) gerritBrowser.getReviewers(changeId, "Code-Review");
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private int getInitialPointForThisFile(String filePath) {
        return INITIAL_POINT;
    }

    private List<List<RevCommit>> lineChangeHistory(String filePath, Set<Integer> lines, List<CommitAndPathWrapper> fileCommitHistory) {
        List<List<RevCommit>> result = new ArrayList<>();

        try {


            Integer[] linesArray = lines.toArray(new Integer[lines.size()]);
            int[][] lineHistoryMatrix = generateLineHistoryMatrix(filePath, linesArray, fileCommitHistory);
            boolean[][] trackingMatrix = generateTrackingMatrix(filePath, linesArray, fileCommitHistory, lineHistoryMatrix);
            List<Set<Integer>> alreadyCheckedLines = new ArrayList<Set<Integer>>();
            for (int x = 0; x < fileCommitHistory.size(); x++) {
                alreadyCheckedLines.add(new HashSet<>());
            }

            for (int x = 0; x < fileCommitHistory.size(); x++) {
                System.out.println(getChangeIdFromFooter(fileCommitHistory.get(x).getRevCommit().getFooterLines()));
            }
            for (int index = 0; index < lines.size(); index++) {
                List<RevCommit> resultForActualLine = new ArrayList<>();
                for (int x = 1; x < fileCommitHistory.size(); x++) {
                    //System.out.println(getChangeIdFromFooter(fileCommitHistory.get(x).getRevCommit().getFooterLines()));
                    if (trackingMatrix[index][x]) {
                        continue;
                    }
                    if (x == fileCommitHistory.size() - 1) {
                        resultForActualLine.add(fileCommitHistory.get(x).getRevCommit());
                        System.out.println("Line " + linesArray[index] + " was initialized in " + fileCommitHistory.get(x).getRevCommit().getShortMessage());
                        break;
                    }
                    if (!fileCommitHistory.get(x).getPath().equals(fileCommitHistory.get(x).getPath())) {
                        x++;
                        continue;
                    }

                    int actualLine = linesArray[index] + lineHistoryMatrix[index][x + 1];

                    RevCommit headCommit = fileCommitHistory.get(x).getRevCommit();
                    RevCommit diffWith = fileCommitHistory.get(x + 1).getRevCommit();
                    EditList edits = gitBrowser.diff(headCommit, diffWith, fileCommitHistory.get(x).getPath());

                    for (Edit edit : edits) {
                        if (edit.getType() == Edit.Type.REPLACE) {
                            //int lineDifferenceInReplace = (edit.getEndA() - edit.getBeginA()) - (edit.getEndB() - edit.getBeginB());
                            if (edit.getBeginA() < actualLine && actualLine <= edit.getEndA()) {
                                resultForActualLine.add(headCommit);
                                System.out.println("Line " + linesArray[index] + " was replaced in " + fileCommitHistory.get(x).getRevCommit().getShortMessage());
                            }
                        }

                        if (edit.getType() == Edit.Type.INSERT) {
                            if (linesArray[index] + lineHistoryMatrix[index][x] >= edit.getBeginB() + 1 && linesArray[index] + lineHistoryMatrix[index][x] <= edit.getEndB())/*(edit.getEndA() + 1 == actualLine)*//*(edit.getBeginA() < actualLine && actualLine <= edit.getEndA() + (edit.getEndB() - edit.getEndA()))*/ {
                            /*if (alreadyCheckedLines.get(x).contains(actualLine)) {
                                continue;
                            }*/
                                alreadyCheckedLines.get(x).add(actualLine);

                                resultForActualLine.add(headCommit);
                                System.out.println("Line " + linesArray[index] + " was inserted in " + fileCommitHistory.get(x).getRevCommit().getShortMessage());
                            }
                        }

                        if (edit.getType() == Edit.Type.DELETE) {
                            if (edit.getBeginA() < actualLine && actualLine <= edit.getEndA()) {
                                //resultForActualLine.add(headCommit);
                                System.out.println("Line " + linesArray[index] + " was deleted in " + fileCommitHistory.get(x).getRevCommit().getShortMessage());
                            }
                        }
                    }

                    //logging
            /*System.out.println("Head: " + headCommit.getShortMessage());
            System.out.println("Diff with: " + diffWith.getShortMessage());
            System.out.println(edits);*/
                }
                result.add(resultForActualLine);
            }
        } catch (IOException ex) {
            //TODO: handle exception
        }

        return result;
    }

    private int[][] generateLineHistoryMatrix(String filePath, Integer[] linesArray, List<CommitAndPathWrapper> fileCommitHistory) throws IOException {
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
            EditList edits = gitBrowser.diff(headCommit, diffWith, fileCommitHistory.get(y).getPath());
            for (Edit edit : edits) {
                if (edit.getType() == Edit.Type.INSERT) {
                    int amountOfInsertedLines = edit.getEndB() - edit.getBeginB();
                    int insertedLinesCounter = 0;
                    for (int lines = 0; lines < linesArray.length; lines++) {
                        if (linesArray[lines] + lineHistoryMatrix[lines][y + 1] > edit.getBeginB() + 1) {
                            /*if (insertedLinesCounter < amountOfInsertedLines) {
                                insertedLinesCounter++;
                            }*/
                            lineHistoryMatrix[lines][y + 1] -= amountOfInsertedLines;//insertedLinesCounter;
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

    private boolean[][] generateTrackingMatrix(String filePath, Integer[] linesArray, List<CommitAndPathWrapper> fileCommitHistory, int[][] lineHistoryMatrix) throws IOException {
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

            EditList edits = gitBrowser.diff(headCommit, diffWith, fileCommitHistory.get(y).getPath());
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

    private Set<Integer> getLinesAffectedByCommit(RevCommit headCommit, RevCommit diffWith, String filePath) {
        try {
            Set<Integer> result = new LinkedHashSet<>();
            EditList edits = gitBrowser.diff(headCommit, diffWith, filePath);

            for (Edit edit : edits) {
                if (edit.getType() == Edit.Type.INSERT) {
                    result.add(getNearestLineOfCode(headCommit, filePath, edit.getBeginB() + 1));
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
            return result;
        } catch (IOException ex) {
            //TODO: handle exception
        }
        return new HashSet<>();
    }

    private int getNearestLineOfCode(RevCommit headCommit, String filePath, int lineNumber) throws IOException {
        //to be changed

        String[] lines = null;
        try {
            Repository repository = gitBrowser.getRepository();
            RevTree tree = headCommit.getTree();

            // try to find a specific file
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filePath));
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);

            // get its content and find nearest line longer than 1
            byte[] contents = loader.getCachedBytes();
            String file = RawParseUtils.decode(contents);
            lines = file.split("\n");

            //loader.copyTo(System.out);
            for (int x = 1; x < lines.length; x++) {
                if (lines[lineNumber - 1 - x].length() > 1 || (lines[lineNumber - 1 - x].length() == 1 && !Character.isWhitespace(lines[lineNumber - 1 - x].charAt(0)))) {
                    return lineNumber - x;
                }
                if (lines[lineNumber - 1 + x].length() > 1 || (lines[lineNumber - 1 - x].length() == 1 && !Character.isWhitespace(lines[lineNumber - 1 - x].charAt(0)))) {
                    return lineNumber + x;
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception" + ex.getMessage());
            System.out.println(lines);
            return lineNumber - 1;
        }

        return lineNumber;
    }

    private int getFileLength(RevCommit headCommit, String filePath) throws IOException {
        Repository repository = gitBrowser.getRepository();
        RevTree tree = headCommit.getTree();

        // try to find a specific file
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        treeWalk.setFilter(PathFilter.create(filePath));
        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(objectId);

        // get its content and find nearest line longer than 1
        byte[] contents = loader.getCachedBytes();
        String file = RawParseUtils.decode(contents);
        String[] lines = file.split("\n");

        return lines.length;
    }

    private String getChangeIdFromFooter(List<FooterLine> commitFooter) {
        if (commitFooter.size() == 0) {
            return "";
        }
        return commitFooter.get(0).getValue();
    }
}
