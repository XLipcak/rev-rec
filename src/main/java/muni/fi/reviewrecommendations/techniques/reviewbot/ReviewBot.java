package muni.fi.reviewrecommendations.techniques.reviewbot;

import muni.fi.reviewrecommendations.common.GitBrowser;
import muni.fi.reviewrecommendations.techniques.revfinder.Review;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ReviewBot: https://labs.vmware.com/download/198/
 *
 * @author Jakub Lipcak, Masaryk University
 */
public class ReviewBot {

    private GitBrowser gitBrowser;
    private static final int INITIAL_POINT = 1;
    private static final double CONSTANT_FACTOR = 0.9;

    public ReviewBot(String gitRepositoryPath) {
        try {
            this.gitBrowser = new GitBrowser(gitRepositoryPath, false);
        } catch (IOException e) {
            e.printStackTrace(); //TODO: should be handled in GitBrowser
        }
    }

    public Map<RevCommit, Double> reviewersRankingAlgorithm(Review review) throws IOException {

        Map<RevCommit, Double> resultMap = new HashMap<>();
        for (String filePath : review.getFilePaths()) {
            List<RevCommit> fileCommitHistory = gitBrowser.getFileCommitHistory(filePath);
            if (fileCommitHistory.size() == 1) {
                continue;
            }
            Set<Integer> lines = getLinesAffectedByCommit(fileCommitHistory.get(0), fileCommitHistory.get(1), filePath);
            List<List<RevCommit>> lch = lineChangeHistory(filePath, lines, fileCommitHistory);
            for (int x = 0; x < lch.size(); x++) {
                double points = getInitialPointForThisFile(filePath);
                for (int y = 0; y < lch.get(x).size(); y++) {
                    RevCommit entry = lch.get(x).get(y);
                    if(resultMap.containsKey(entry)){
                        resultMap.replace(entry, resultMap.get(entry) + points);
                    } else {
                        resultMap.put(entry, points);
                    }
                    points *= CONSTANT_FACTOR;
                }
            }
        }

       /* Iterator it = resultMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " - " + pair.getValue());
            it.remove();
        }*/

        System.out.println(propagateResultToUserPoints(resultMap));
        return resultMap;
    }


    private Map<String, Double> propagateResultToUserPoints(Map<RevCommit, Double> pointsMap){
        Map<String, Double> reviewerCandidates = new HashMap<>();

        Iterator it = pointsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            for (String user : getUserRelatedToCommit((RevCommit) pair.getKey())){
                if(reviewerCandidates.containsKey(user)){
                    reviewerCandidates.replace(user, reviewerCandidates.get(user) + (Double) pair.getValue());
                } else {
                    reviewerCandidates.put(user, (Double) pair.getValue());
                }
            }
            it.remove();
        }

        return sortByValue(reviewerCandidates);
    }

    private List<String> getUserRelatedToCommit(RevCommit commit){
        Random rand = new Random();
        int  n = rand.nextInt(50) + 1;
        List<String> result = new ArrayList<>();
        //TODO...
        if(n>25){
            result.add("User 1");
        }else{
            result.add("User 2");
        }

        return result;
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

    private int getInitialPointForThisFile(String filePath){
        return INITIAL_POINT;
    }

    private List<List<RevCommit>> lineChangeHistory(String filePath, Set<Integer> lines, List<RevCommit> fileCommitHistory) throws IOException {
        List<List<RevCommit>> result = new ArrayList<>();

        Integer[] linesArray = lines.toArray(new Integer[lines.size()]);
        int[][] lineHistoryMatrix = generateLineHistoryMatrix(filePath, linesArray, fileCommitHistory);

        for (int index = 0; index < lines.size(); index++) {
            List<RevCommit> resultForActualLine = new ArrayList<>();
            for (int x = 0; x < fileCommitHistory.size(); x++) {
                int actualLine = linesArray[index] + lineHistoryMatrix[index][x];

                if (x == fileCommitHistory.size() - 1) {
                    resultForActualLine.add(fileCommitHistory.get(x));
                    System.out.println("Line " + actualLine + " was initialized in " + fileCommitHistory.get(x));
                    break;
                }

                RevCommit headCommit = fileCommitHistory.get(x);
                RevCommit diffWith = fileCommitHistory.get(x + 1);
                EditList edits = gitBrowser.diff(headCommit, diffWith, filePath);

                for (Edit edit : edits) {
                    if (edit.getType() == Edit.Type.REPLACE) {
                        if (edit.getBeginA() < actualLine && actualLine <= edit.getEndB()) {
                            resultForActualLine.add(headCommit);
                            System.out.println("Line " + actualLine + " was replaced in " + fileCommitHistory.get(x));
                        }
                    }

                    if (edit.getType() == Edit.Type.INSERT) {
                        if (edit.getBeginA() < actualLine && actualLine <= edit.getEndB()) {
                            resultForActualLine.add(headCommit);
                            System.out.println("Line " + actualLine + " was inserted in " + fileCommitHistory.get(x));
                        }
                    }

                    if (edit.getType() == Edit.Type.DELETE) {
                        if (edit.getBeginA() < actualLine && actualLine <= edit.getEndB()) {
                            resultForActualLine.add(headCommit);
                            System.out.println("Line " + actualLine + " was deleted in " + fileCommitHistory.get(x));
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

        return result;
    }

    private int[][] generateLineHistoryMatrix(String filePath, Integer[] linesArray, List<RevCommit> fileCommitHistory) throws IOException {
        int[][] lineHistoryMatrix = new int[linesArray.length][fileCommitHistory.size()];
        for (int y = 0; y < fileCommitHistory.size(); y++) {
            if (y == fileCommitHistory.size() - 1) {
                break;
            }
            RevCommit headCommit = fileCommitHistory.get(y);
            RevCommit diffWith = fileCommitHistory.get(y + 1);
            EditList edits = gitBrowser.diff(headCommit, diffWith, filePath);
            for (Edit edit : edits) {
                if (edit.getType() == Edit.Type.INSERT) {
                    int amountOfInsertedLines = edit.getEndB() - edit.getBeginB();
                    for (int lines = 0; lines < linesArray.length; lines++) {
                        if (linesArray[lines] + lineHistoryMatrix[lines][y] > edit.getBeginB() + 1) {
                            lineHistoryMatrix[lines][y + 1] -= amountOfInsertedLines;
                        }
                    }
                }

                if (edit.getType() == Edit.Type.DELETE) {
                    int amountOfDeletedLines = edit.getEndA() - edit.getBeginA();
                    for (int lines = 0; lines < linesArray.length; lines++) {
                        if (linesArray[lines] + lineHistoryMatrix[lines][y] > edit.getEndA()) {
                            lineHistoryMatrix[lines][y + 1] += amountOfDeletedLines;
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

    private Set<Integer> getLinesAffectedByCommit(RevCommit headCommit, RevCommit diffWith, String filePath) throws
            IOException {
        Set<Integer> result = new LinkedHashSet<>();
        EditList edits = gitBrowser.diff(headCommit, diffWith, filePath);
        for (Edit edit : edits) {
            if (edit.getType() == Edit.Type.INSERT) {
                result.add(edit.getBeginB() + 1);
            }
            if (edit.getType() == Edit.Type.DELETE) {
                result.add(edit.getBeginA() + 1);
            }
            if (edit.getType() == Edit.Type.REPLACE) {
                for (int x = edit.getBeginB() + 1; x <= edit.getEndB(); x++) {
                    result.add(x);
                }
            }
        }
        return result;
    }
}
