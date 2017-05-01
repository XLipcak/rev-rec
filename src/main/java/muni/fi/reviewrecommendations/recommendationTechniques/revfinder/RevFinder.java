package muni.fi.reviewrecommendations.recommendationTechniques.revfinder;

import muni.fi.reviewrecommendations.db.model.filePath.FilePath;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequest;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import muni.fi.reviewrecommendations.recommendationTechniques.ReviewerRecommendation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of RevFinder: http://ieeexplore.ieee.org/document/7081824/
 *
 * @author Jakub Lipcak, Masaryk University
 */
public class RevFinder implements ReviewerRecommendation {

    private static final int LONGEST_COMMON_PREFIX = 0;
    private static final int LONGEST_COMMON_SUFFIX = 1;
    private static final int LONGEST_COMMON_SUBSTRING = 2;
    private static final int LONGEST_COMMON_SUBSEQUENCE = 3;

    private static final boolean useSubProjectName = false;

    private List<PullRequest> allPreviousReviews;


    public RevFinder(List<PullRequest> allPreviousReviews) {
        if (useSubProjectName) {
            allPreviousReviews.forEach(this::modifyPullRequestFilePaths);
        }
        this.allPreviousReviews = allPreviousReviews;
    }

    @Override
    public void buildModel() {
    }

    @Override
    public Map<Reviewer, Double> recommend(PullRequest pullRequest) {
        if (useSubProjectName) {
            modifyPullRequestFilePaths(pullRequest);
        }

        ArrayList<HashMap<Reviewer, Double>> reviewerCandidates = new ArrayList<>();
        for (int stringComparisonTechnique = 0; stringComparisonTechnique < 4;
             stringComparisonTechnique++) {
            reviewerCandidates.add(new HashMap<>());
            for (PullRequest rev : allPreviousReviews) {
                Set<FilePath> newReviewFilePaths = pullRequest.getFilePaths();
                Set<FilePath> revFilePaths = rev.getFilePaths();
                double score = 0;

                for (FilePath newReviewFilePath : newReviewFilePaths) {
                    for (FilePath revFilePath : revFilePaths) {
                        score += filePathSimilarity(newReviewFilePath.getLocation(),
                                revFilePath.getLocation(), stringComparisonTechnique);
                    }
                }

                score = score / (double) (newReviewFilePaths.size() * revFilePaths.size());
                if (Double.isNaN(score)) {
                    score = 0;
                }

                for (Reviewer codeReviewer : rev.getReviewers()) {
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

    private Map<Reviewer, Double> bordaCountCombination(ArrayList<HashMap<Reviewer, Double>> reviewerCandidates) {
        Map<Reviewer, Double> result = new HashMap<>();

        for (int x = 0; x < 4; x++) {
            double counter = 0;
            Map<Reviewer, Double> actualCandidates = sortByValue(reviewerCandidates.get(x));

            //find non zero entries
            for (Map.Entry<Reviewer, Double> entry : actualCandidates.entrySet()) {
                if (entry.getValue() > 0) {
                    counter++;
                }
            }

            //compute Borda combination
            for (Map.Entry<Reviewer, Double> entry : actualCandidates.entrySet()) {
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

    // File Path Comparison recommendationTechniques:
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

    private String removeSlash(String filePath) {
        String result = "";

        for (int x = 0; x < filePath.length(); x++) {
            if (filePath.charAt(x) != '/') {
                result += filePath.charAt(x);
            }
        }
        return result;
    }

    private void modifyPullRequestFilePaths(PullRequest pullRequest) {
        Set<FilePath> filePaths = new HashSet<>();
        pullRequest.getFilePaths().forEach(x -> filePaths.add(new FilePath(removeSlash(pullRequest.getSubProject()) + "/" + x.getLocation())));
        pullRequest.setFilePaths(filePaths);
    }

    public List<PullRequest> getAllPreviousReviews() {
        return allPreviousReviews;
    }

    public void setAllPreviousReviews(List<PullRequest> allPreviousReviews) {
        this.allPreviousReviews = allPreviousReviews;
    }
}
