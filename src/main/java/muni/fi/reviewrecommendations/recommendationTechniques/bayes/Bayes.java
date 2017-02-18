package muni.fi.reviewrecommendations.recommendationTechniques.bayes;

import muni.fi.reviewrecommendations.db.model.filePath.FilePath;
import muni.fi.reviewrecommendations.db.model.filePath.FilePathDAO;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequest;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestDAO;
import muni.fi.reviewrecommendations.db.model.reviewer.Reviewer;
import muni.fi.reviewrecommendations.recommendationTechniques.Review;
import muni.fi.reviewrecommendations.recommendationTechniques.ReviewerRecommendation;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.inference.jtree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.util.NumericalInstabilityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Service
public class Bayes implements ReviewerRecommendation {

    private static final double MINIMAL_PERCENTAGE_VALUE = 0.01;

    @Autowired
    private PullRequestDAO pullRequestDAO;

    @Autowired
    private FilePathDAO filePathDAO;

    @Value("${recommendation.project}")
    private String project;

    //Bayes network attributes
    private IBayesInferer inferer;
    private BayesNode reviewersNode;
    private BayesNode subProjectNode;
    private BayesNode filePathNode;
    private BayesNode ownerNode;

    private List<Reviewer> allReviewers;
    List<String> allFilePaths;
    List<Reviewer> allOwners;
    List<String> allSubProjects;

    public Bayes() {
    }

    public void buildNetwork(Long timeStamp) {
        allFilePaths = findAllFilePaths(timeStamp);
        allOwners = findAllOwners(timeStamp);
        allReviewers = findAllCodeReviewers(timeStamp);
        allSubProjects = findAllSubProjects(timeStamp);
        int index;

        //Build Bayesian network
        BayesNet net = new BayesNet();


        //Reviewers node
        reviewersNode = net.createNode("reviewersNode");
        String reviewersOutcomes[] = new String[allReviewers.size()];
        double[] reviewersProbabilities = new double[allReviewers.size()];
        double allReviewersSize = (double) pullRequestDAO.findByProjectNameAndTimeLessThan(project, timeStamp).size();
        index = 0;
        for (Reviewer reviewer : allReviewers) {
            reviewersOutcomes[index] = reviewer.getId().toString();
            reviewersProbabilities[index] = ((double) (pullRequestDAO.findByAllSpecificCodeReviewersAndProjectNameAndTimeLessThan(reviewer, project, timeStamp).size()) / allReviewersSize);
            index++;
            //System.out.println(index);
        }
        reviewersNode.addOutcomes(reviewersOutcomes);
        reviewersNode.setProbabilities(reviewersProbabilities);

        //Subproject node
        subProjectNode = net.createNode("subProjectNode");
        String subProjectOutcomes[] = new String[allSubProjects.size()];
        double[] subProjectProbabilities = new double[allReviewers.size() * allSubProjects.size()];
        String[] subProjectsArray = allSubProjects.toArray(new String[allSubProjects.size()]);
        index = 0;
        int subProjectIndex = 0;
        for (String subProject : subProjectsArray) {
            subProjectOutcomes[subProjectIndex] = subProject;
            subProjectIndex++;
        }

        for (Reviewer reviewer : allReviewers) {
            double denumeratorSize = (double) pullRequestDAO.findByProjectNameAndAllSpecificCodeReviewersAndTimeLessThan(project, reviewer, timeStamp).size();
            for (String subProject : subProjectsArray) {
                subProjectProbabilities[index] = ((double) pullRequestDAO.findByProjectNameAndSubProjectAndAllSpecificCodeReviewersAndTimeLessThan(project, subProject, reviewer, timeStamp).size() /
                        denumeratorSize);
                index++;
            }
            subProjectProbabilities = normaliseArray(index - subProjectsArray.length, index, subProjectProbabilities);
        }

        subProjectNode.addOutcomes(subProjectOutcomes);
        subProjectNode.setParents(Arrays.asList(reviewersNode));
        subProjectNode.setProbabilities(subProjectProbabilities);


        //File path node
        filePathNode = net.createNode("filePathNode");
        String[] filePathNodeOutcomes = allFilePaths.toArray(new String[allFilePaths.size()]);
        double[] filePathNodeProbabilities = new double[allReviewers.size() * allFilePaths.size()];
        index = 0;
        for (Reviewer reviewer : allReviewers) {
            double denumeratorSize = (double) filePathDAO.findByPullRequestProjectNameAndPullRequestAllSpecificCodeReviewersAndPullRequestTimeLessThan(project, reviewer, timeStamp).size();
            for (String filePath : filePathNodeOutcomes) {
                filePathNodeProbabilities[index] = //1d / filePathNodeOutcomes.length;
                        filePathDAO.findByPullRequestProjectNameAndFilePathAndPullRequestAllSpecificCodeReviewersAndPullRequestTimeLessThan(project,
                                filePath, reviewer, timeStamp).size() / denumeratorSize;
                index++;
                System.out.println(index + "/" + filePathNodeProbabilities.length);
            }
            filePathNodeProbabilities = normaliseArray(index - filePathNodeOutcomes.length, index, filePathNodeProbabilities);
        }
        filePathNode.addOutcomes(filePathNodeOutcomes);
        filePathNode.setParents(Arrays.asList(reviewersNode));
        filePathNode.setProbabilities(filePathNodeProbabilities);


        //Owner node
        ownerNode = net.createNode("ownerNode");
        String[] ownerNodeOutcomes = new String[allOwners.size()];
        int ownerIndex = 0;
        for (Reviewer owner : allOwners) {
            ownerNodeOutcomes[ownerIndex] = owner.getId().toString();
            ownerIndex++;
        }
        double[] ownerNodeProbabilities = new double[allReviewers.size() * allOwners.size()];
        index = 0;
        for (Reviewer reviewer : allReviewers) {
            double denumeratorSize = (double) (pullRequestDAO.findByProjectNameAndAllSpecificCodeReviewersAndTimeLessThan(project, reviewer, timeStamp).size());
            for (Reviewer owner : allOwners) {
                ownerNodeProbabilities[index] = (double) pullRequestDAO.findByProjectNameAndAllSpecificCodeReviewersAndOwnerAndTimeLessThan(project, reviewer, owner, timeStamp).size() /
                        denumeratorSize;
                index++;
            }
            ownerNodeProbabilities = normaliseArray(index - allOwners.size(), index, ownerNodeProbabilities);
        }

        ownerNode.addOutcomes(ownerNodeOutcomes);
        ownerNode.setParents(Arrays.asList(reviewersNode));
        ownerNode.setProbabilities(ownerNodeProbabilities);


        inferer = new JunctionTreeAlgorithm();
        inferer.setNetwork(net);
    }

    @Override
    public Map<Reviewer, Double> reviewersRankingAlgorithm(Review review) {
        Map<Reviewer, Double> result = new HashMap<>();
        for (String x : review.getFilePaths()) {
            Map<Reviewer, Double> resultList = recommend(review, x);
            resultList = sortByValue(resultList);
            double y = resultList.size();
            for (Map.Entry<Reviewer, Double> entry : resultList.entrySet()) {
                if (result.containsKey(entry.getKey())) {
                    result.replace(entry.getKey(), result.get(entry.getKey()) + y);
                } else {
                    result.put(entry.getKey(), y);
                }
                y--;
            }
        }
        return result;
    }

    private Map<Reviewer, Double> recommend(Review review, String fileEnding) {
        Map<Reviewer, Double> result = new HashMap<>();

        Map<BayesNode, String> evidence = new HashMap<BayesNode, String>();

        if (new HashSet<>(allSubProjects).contains(review.getSubProject())) {
            evidence.put(subProjectNode, review.getSubProject());
        } else {
            System.out.println("Bayes otherSubProject");
            evidence.put(subProjectNode, "otherSubProject");
        }

        if (new HashSet<>(allFilePaths).contains(fileEnding)) {
            evidence.put(filePathNode, fileEnding);
        } else {
            System.out.println("Bayes otherFilePath");
            evidence.put(filePathNode, "otherFilePath");
        }


        if (new HashSet<>(allOwners).contains(review.getOwner())) {
            evidence.put(ownerNode, review.getOwner().getId().toString());
        } else {
            System.out.println("Bayes -1");
            evidence.put(ownerNode, "-1");
        }

        inferer.setEvidence(evidence);

        try {
            double[] beliefs = inferer.getBeliefs(reviewersNode);
            for (int x = 0; x < beliefs.length; x++) {
                result.put(allReviewers.get(x), beliefs[x]);
            }

            return result;
        } catch (NumericalInstabilityException ex) {
            System.out.println(ex.getMessage());
            return result;
        }
    }


    private List<Reviewer> findAllCodeReviewers(Long timeStamp) {
        Set<Reviewer> allReviewers = new HashSet<>();

        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimeLessThan(project, timeStamp)) {
            for (Reviewer reviewer : pullRequest.getAllSpecificCodeReviewers()) {
                allReviewers.add(reviewer);
            }
        }

        return new ArrayList<>(allReviewers);
    }

    public List<Reviewer> findAllOwners(Long timeStamp) {
        Set<Reviewer> allOwners = new HashSet<>();

        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimeLessThan(project, timeStamp)) {
            allOwners.add(pullRequest.getOwner());
        }

        //add other reviewer
        Reviewer reviewer = new Reviewer();
        reviewer.setId(-1);
        allOwners.add(reviewer);

        return new ArrayList<>(allOwners);
    }

    public List<String> findAllSubProjects(Long timeStamp) {
        Set<String> allSubProjects = new HashSet<>();
        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimeLessThan(project, timeStamp)) {
            allSubProjects.add(pullRequest.getSubProject());
        }
        allSubProjects.add("otherSubProject");

        return new ArrayList<>(allSubProjects);
    }

    private double[] normaliseArray(int beginIndex, int endIndex, double[] array) {
        int nonZeroElements = 0;
        int zeroElements = 0;
        for (int x = beginIndex; x < endIndex; x++) {
            if (array[x] > 0) {
                nonZeroElements++;
            } else {
                zeroElements++;
            }
        }
        for (int x = beginIndex; x < endIndex; x++) {
            if (array[x] > 0) {
                array[x] = array[x] - (MINIMAL_PERCENTAGE_VALUE / nonZeroElements);
            } else {
                array[x] = array[x] + (MINIMAL_PERCENTAGE_VALUE / zeroElements);
            }
        }

        return array;
    }

    private List<String> findAllFilePaths(long timeStamp) {
        Set<String> allFilePaths = new HashSet<>();
        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimeLessThan(project, timeStamp)) {
            for (FilePath filePath : pullRequest.getFilePaths()) {
                allFilePaths.add(filePath.getFilePath());
            }
        }
        allFilePaths.add("otherFilePath");

        return new ArrayList<>(allFilePaths);
    }


    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
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
}
