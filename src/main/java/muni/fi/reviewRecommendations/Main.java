package muni.fi.reviewRecommendations;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import java.util.List;
import muni.fi.reviewRecommendations.techniques.revfinder.Review;
import muni.fi.reviewRecommendations.techniques.revfinder.RevFinder;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kubo
 */
public class Main {

    /***
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        /*RevFinder revFinder = new RevFinder();

        Review review3 = new Review();
        String[] filePaths3 = {"a/b/ddd/123"};
        review3.setFilePaths(filePaths3);
        Map<String, Integer> map = revFinder.reviewersRankingAlgorithm(review3);

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }*/

        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritAuthData.Basic authData = new GerritAuthData.Basic("https://android-review.googlesource.com");
        GerritApi gerritApi = gerritRestApiFactory.create(authData);
        List<ChangeInfo> changes = null;
        try {
            changes = gerritApi.changes().query("change:31760&o=DETAILED_LABELS").withLimit(10).get();
        } catch (RestApiException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

}
