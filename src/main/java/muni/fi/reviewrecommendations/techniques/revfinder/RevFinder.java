package muni.fi.reviewrecommendations.techniques.revfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of RevFinder: http://ieeexplore.ieee.org/document/7081824/
 *
 * @author Jakub Lipcak, Masaryk University
 */
public class RevFinder {

    private static final int LONGEST_COMMON_PREFIX = 0;
    private static final int LONGEST_COMMON_SUFFIX = 1;
    private static final int LONGEST_COMMON_SUBSTRING = 2;
    private static final int LONGEST_COMMON_SUBSEQUENCE = 3;

    private Review[] allPreviousReviews;
    private String[] reviewers;

    public RevFinder() {
        //TODO: initialization from some datasource

        //init test data
        Review review1 = new Review();
        String[] filePaths1 = {"a/b/c", "a/123"};
        String[] reviewers1 = {"X1"};
        review1.setFilePaths(filePaths1);
        review1.setReviewers(reviewers1);

        Review review2 = new Review();
        String[] filePaths2 = {"a/b/c", "a/b/123"};
        String[] reviewers2 = {"X2", "X3"};
        review2.setFilePaths(filePaths2);
        review2.setReviewers(reviewers2);

        allPreviousReviews = new Review[2];
        allPreviousReviews[0] = review1;
        allPreviousReviews[1] = review2;
    }

    public Map<String, Integer> reviewersRankingAlgorithm(Review review) {
        ArrayList<HashMap<String, Double>> reviewerCandidates = new ArrayList<>();
        for (int stringComparisonTechnique = 0; stringComparisonTechnique < 4;
                stringComparisonTechnique++) {
            reviewerCandidates.add(new HashMap<>());
            for (Review rev : allPreviousReviews) {
                String[] newReviewFilePaths = review.getFilePaths();
                String[] revFilePaths = rev.getFilePaths();
                double score = 0;

                for (String newReviewFilePath : newReviewFilePaths) {
                    for (String revFilePath : revFilePaths) {
                        score += filePathSimilarity(newReviewFilePath,
                                revFilePath, stringComparisonTechnique);
                    }
                }

                score = score / (double) (newReviewFilePaths.length * revFilePaths.length);

                for (String codeReviewer : rev.getReviewers()) {
                    if (reviewerCandidates.get(stringComparisonTechnique).containsKey(codeReviewer)) {
                        reviewerCandidates.get(stringComparisonTechnique)
                                .replace(codeReviewer,
                                        reviewerCandidates.get(stringComparisonTechnique).get(codeReviewer) + score);
                    } else {
                        reviewerCandidates.get(stringComparisonTechnique).put(codeReviewer, score);
                    }
                }
            }
        }

        return bordaCountCombination(reviewerCandidates);
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

    private Map<String, Integer> bordaCountCombination(ArrayList<HashMap<String, Double>> reviewerCandidates) {
        Map<String, Integer> result = new HashMap<>();

        for (int x = 0; x < 4; x++) {
            int counter = 0;
            Map<String, Double> actualCandidates = sortByValue(reviewerCandidates.get(0));

            //find non zero entries
            for (Map.Entry<String, Double> entry : actualCandidates.entrySet()) {
                if (entry.getValue() > 0) {
                    counter++;
                }
            }

            //compute Borda combination
            for (Map.Entry<String, Double> entry : actualCandidates.entrySet()) {
                if (counter == 0) {
                    break;
                }
                if (result.containsKey(entry.getKey())) {
                    result.replace(entry.getKey(), result.get(entry.getKey()) + counter);
                } else {
                    result.put(entry.getKey(), counter);
                }
                counter--;
            }
        }

        return result;
    }

    private double filePathSimilarity(String path1, String path2,
            int comparisonTechnique) {

        int score = 0;
        switch (comparisonTechnique) {
            case LONGEST_COMMON_PREFIX:
                score = longestCommonPrefix(path1, path2);
                break;
            case LONGEST_COMMON_SUFFIX:
                score = longestCommonSuffix(path1, path2);
                break;
            case LONGEST_COMMON_SUBSTRING:
                score = longestCommonSubstring(path1, path2);
                break;
            case LONGEST_COMMON_SUBSEQUENCE:
                score = longestCommonSubsequence(path1, path2);
                break;
        }

        return score / (double) (Math.max(path1.split("/").length, path2.split("/").length));
    }

    // File Path Comparison techniques:
    private int longestCommonPrefix(String path1, String path2) {

        String[] path1Array = path1.split("/");
        String[] path2Array = path2.split("/");
        int result = 0;

        for (; result < Math.min(path1Array.length, path2Array.length); result++) {
            if (!path1Array[result].equals(path2Array[result])) {
                return result;
            }
        }
        return result;
    }

    private int longestCommonSuffix(String path1, String path2) {

        String[] path1Array = path1.split("/");
        String[] path2Array = path2.split("/");
        int result = 0;

        for (; result < Math.min(path1Array.length, path2Array.length); result++) {
            if (!path1Array[path1Array.length - result - 1].equals(
                    path2Array[path2Array.length - result - 1])) {
                return result;
            }
        }
        return result;
    }

    private int longestCommonSubstring(String path1, String path2) {

        String[] path1Array = path1.split("/");
        String[] path2Array = path2.split("/");
        int m = path1Array.length;
        int n = path2Array.length;

        int maxValue = 0;
        int[][] dpMatrix = new int[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (path1Array[i].equals(path2Array[j])) {
                    if (i == 0 || j == 0) {
                        dpMatrix[i][j] = 1;
                    } else {
                        dpMatrix[i][j] = dpMatrix[i - 1][j - 1] + 1;
                    }

                    if (maxValue < dpMatrix[i][j]) {
                        maxValue = dpMatrix[i][j];
                    }
                }

            }
        }

        return maxValue;
    }

    private int longestCommonSubsequence(String path1, String path2) {

        String[] path1Array = path1.split("/");
        String[] path2Array = path2.split("/");
        int m = path1Array.length;
        int n = path2Array.length;

        int[][] dpMatrix = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0) {
                    dpMatrix[i][j] = 0;
                } else if (path1Array[i - 1].equals(path2Array[j - 1])) {
                    dpMatrix[i][j] = 1 + dpMatrix[i - 1][j - 1];
                } else {
                    dpMatrix[i][j] = Math.max(dpMatrix[i - 1][j], dpMatrix[i][j - 1]);
                }
            }
        }

        return dpMatrix[m][n];
    }
}
