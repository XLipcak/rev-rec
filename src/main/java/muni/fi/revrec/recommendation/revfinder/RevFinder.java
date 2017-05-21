package muni.fi.revrec.recommendation.revfinder;

import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.pullRequest.PullRequestDAO;
import muni.fi.revrec.model.reviewer.Developer;
import muni.fi.revrec.recommendation.ReviewerRecommendation;
import muni.fi.revrec.recommendation.ReviewerRecommendationBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Implementation of RevFinder algorithm: http://ieeexplore.ieee.org/document/7081824/
 *
 * @author Jakub Lipcak, Masaryk University
 */
@Service
public class RevFinder extends ReviewerRecommendationBase implements ReviewerRecommendation {

    private static final int LONGEST_COMMON_PREFIX = 0;
    private static final int LONGEST_COMMON_SUFFIX = 1;
    private static final int LONGEST_COMMON_SUBSTRING = 2;
    private static final int LONGEST_COMMON_SUBSEQUENCE = 3;

    private boolean useSubProjectName;
    private List<PullRequest> allPreviousReviews;

    @Autowired
    public RevFinder(@Autowired PullRequestDAO pullRequestDAO,
                     @Value("${recommendation.retired}") boolean removeRetiredReviewers,
                     @Value("${recommendation.retired.interval}") long timeRetiredInMonths,
                     @Value("${recommendation.project}") String project,
                     @Value("${recommendation.revfinder.projectname}") boolean useSubProjectName) {
        super(pullRequestDAO, removeRetiredReviewers, timeRetiredInMonths, project);
        init(pullRequestDAO.findByProjectNameOrderByTimestampDesc(project), useSubProjectName);
    }


    /**
     * Initialize all previous pull requests and process their file paths which will be used for the recommendation.
     *
     * @param allPreviousReviews all previously reviewed pull requests.
     * @param useSubProjectName  set, whether sub-projects' names without slashes should be added at the beginning of every file path.
     */
    private void init(List<PullRequest> allPreviousReviews, boolean useSubProjectName) {
        if (useSubProjectName) {
            allPreviousReviews.forEach(this::modifyPullRequestFilePaths);
        }
        this.allPreviousReviews = allPreviousReviews;
        this.useSubProjectName = useSubProjectName;
    }

    @Override
    public void buildModel() {
    }

    @Override
    public List<Developer> recommend(PullRequest pullRequest) {
        if (useSubProjectName) {
            modifyPullRequestFilePaths(pullRequest);
        }

        ArrayList<HashMap<Developer, Double>> reviewerCandidates = new ArrayList<>();
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

                for (Developer codeReviewer : rev.getReviewer()) {
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

        return processResult(bordaCountCombination(reviewerCandidates), pullRequest);
    }

    /**
     * Combine scores assigned to code reviewer candidates using the Borda count combination method.
     *
     * @param reviewerCandidates list of reviewer candidates and scores assigned to these candidates by four string comparison techniques.
     * @return code reviewer and their scores assigned by the Borda count combination method.
     */
    private Map<Developer, Double> bordaCountCombination(ArrayList<HashMap<Developer, Double>> reviewerCandidates) {
        Map<Developer, Double> result = new HashMap<>();

        for (int x = 0; x < 4; x++) {
            double counter = 0;
            Map<Developer, Double> actualCandidates = sortByValue(reviewerCandidates.get(x));

            //find non zero entries
            for (Map.Entry<Developer, Double> entry : actualCandidates.entrySet()) {
                if (entry.getValue() > 0) {
                    counter++;
                }
            }

            //compute Borda combination
            for (Map.Entry<Developer, Double> entry : actualCandidates.entrySet()) {
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

    /**
     * Find the similarity between two file paths using the specified string comparison method.
     *
     * @param path1               first string.
     * @param path2               second string.
     * @param comparisonTechnique string comparison method.
     * @return score assigned by the specified string comparison method.
     */
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


    /**
     * Find the longest common prefix of two strings.
     *
     * @param path1 first string.
     * @param path2 second string.
     * @return longest common prefix in the file paths of path1 and path2.
     */
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

    /**
     * Find the longest common suffix of two strings.
     *
     * @param path1 first string.
     * @param path2 second string.
     * @return longest common suffix in the file paths of path1 and path2.
     */
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

    /**
     * Find the longest common substring of two strings.
     *
     * @param path1 first string.
     * @param path2 second string.
     * @return longest common substring in the file paths of path1 and path2.
     */
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

    /**
     * Find the longest common subsequence of two strings.
     *
     * @param path1 first string.
     * @param path2 second string.
     * @return longest common subsequence in the file paths of path1 and path2.
     */
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

    /**
     * Remove slashes from the given string.
     *
     * @param text original text.
     * @return text without slashes.
     */
    private String removeSlashes(String text) {
        String result = "";

        for (int x = 0; x < text.length(); x++) {
            if (text.charAt(x) != '/') {
                result += text.charAt(x);
            }
        }
        return result;
    }

    /**
     * Add name of the sub-project without slashes to the beginning of every file path of the pull request.
     *
     * @param pullRequest pull request to be modified.
     */
    private void modifyPullRequestFilePaths(PullRequest pullRequest) {
        Set<FilePath> filePaths = new HashSet<>();
        pullRequest.getFilePaths().forEach(x -> filePaths.add(new FilePath(removeSlashes(pullRequest.getSubProject()) + "/" + x.getLocation())));
        pullRequest.setFilePaths(filePaths);
    }

    public List<PullRequest> getAllPreviousReviews() {
        return allPreviousReviews;
    }

    public void setAllPreviousReviews(List<PullRequest> allPreviousReviews) {
        this.allPreviousReviews = allPreviousReviews;
    }
}
