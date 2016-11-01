package muni.fi.reviewRecommendations;

import muni.fi.reviewRecommendations.techniques.revfinder.Review;
import muni.fi.reviewRecommendations.techniques.revfinder.RevFinder;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Kubo
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        RevFinder revFinder = new RevFinder();

        Review review3 = new Review();
        String[] filePaths3 = {"a/b/ddd/123"};
        review3.setFilePaths(filePaths3);
        Map<String, Integer> map = revFinder.reviewersRankingAlgorithm(review3);

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }
    }

}
