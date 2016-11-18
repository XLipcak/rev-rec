package muni.fi.reviewrecommendations.techniques.reviewbot;

import muni.fi.reviewrecommendations.common.GitBrowser;
import muni.fi.reviewrecommendations.techniques.revfinder.Review;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.*;

/**
 * Implementation of ReviewBot: https://labs.vmware.com/download/198/
 *
 * @author Jakub Lipcak, Masaryk University
 */
public class ReviewBot {

    private GitBrowser gitBrowser;

    public ReviewBot(String gitRepositoryPath) {
        try {
            this.gitBrowser = new GitBrowser(gitRepositoryPath, false);
        } catch (IOException e) {
            e.printStackTrace(); //TODO: should be handled in GitBrowser
        }
    }

    public Map<String, Integer> reviewersRankingAlgorithm(Review review) throws IOException {
        Map<String, Integer> result = new HashMap<>();

        for (String filePath : review.getFilePaths()) {
            List<RevCommit> fileCommitHistory = gitBrowser.getFileCommitHistory(filePath);
            for (int actualLine : getLinesAffectedByCommit(fileCommitHistory.get(0), fileCommitHistory.get(1), filePath)) {
                lineChangeHistory(filePath, actualLine, fileCommitHistory);
            }
        }

        return result;
    }

    private List<Review> lineChangeHistory(String filePath, int line, List<RevCommit> fileCommitHistory) throws IOException {
        List<Review> result = new ArrayList<>();


        for (int x = 0; x < fileCommitHistory.size(); x++) {
            if(x == fileCommitHistory.size()-1){
                //initial commit affects all lines
                System.out.println("Line " + line + " was replaced in commit " + (x));
                break;
            }
            RevCommit headCommit = fileCommitHistory.get(x);
            RevCommit diffWith = fileCommitHistory.get(x + 1);
            EditList edits = gitBrowser.diff(headCommit, diffWith, filePath);
            for(Edit edit : edits){
                //replace
                if(edit.getType() == Edit.Type.REPLACE){
                    if(edit.getBeginA() < line && line <= edit.getEndB()){
                        //line was affected by replace in this commit
                        System.out.println("Line " + line + " was replaced in commit " + (x));
                    }
                }
                //TODO: handle all types of diff changes
            }

            //logging
            /*System.out.println("Head: " + headCommit.getShortMessage());
            System.out.println("Diff with: " + diffWith.getShortMessage());
            System.out.println(edits);*/
        }

        return result;
    }

    private Set<Integer> getLinesAffectedByCommit(RevCommit headCommit, RevCommit diffWith, String filePath) throws IOException {
        Set<Integer> result = new HashSet<>();
        EditList edits = gitBrowser.diff(headCommit, diffWith, filePath);
        for(Edit edit : edits){
            for(int x = edit.getBeginB() + 1; x <= edit.getEndB(); x++){
                result.add(x);
            }
        }
        return result;
    }
}
