package muni.fi.reviewrecommendations.recommendationTechniques.bayes;

import muni.fi.reviewrecommendations.db.model.filePath.FilePath;
import muni.fi.reviewrecommendations.db.model.filePath.FilePathDAO;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequest;
import muni.fi.reviewrecommendations.db.model.pullRequest.PullRequestDAO;
import muni.fi.reviewrecommendations.db.model.reviewer.Developer;
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
 * Implementation of reviewer recommendation with the usage of Naive Bayes.
 *
 * @author Jakub Lipcak, Masaryk University
 */
@Service
public class BayesRec implements ReviewerRecommendation {

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

    private List<Developer> allReviewers;
    List<String> allFilePaths;
    List<Developer> allOwners;
    List<String> allSubProjects;

    public BayesRec() {
    }

    @Override
    public void buildModel() {
        buildModel(Long.MAX_VALUE);
    }

    public void buildModel(Long timestamp) {
        allFilePaths = findAllFilePaths(timestamp);
        allOwners = findAllOwners(timestamp);
        allReviewers = findAllCodeReviewers(timestamp);
        allSubProjects = findAllSubProjects(timestamp);
        int index;

        //Build Bayesian network
        BayesNet net = new BayesNet();


        //Reviewers node
        reviewersNode = net.createNode("reviewersNode");
        String reviewersOutcomes[] = new String[allReviewers.size()];
        double[] reviewersProbabilities = new double[allReviewers.size()];
        double allReviewersSize = (double) pullRequestDAO.findByProjectNameAndTimestampLessThan(project, timestamp).size();
        index = 0;
        for (Developer reviewer : allReviewers) {
            reviewersOutcomes[index] = reviewer.getId().toString();
            reviewersProbabilities[index] = ((double) (pullRequestDAO.findByReviewerAndProjectNameAndTimestampLessThan(reviewer, project, timestamp).size()) / allReviewersSize);
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

        for (Developer reviewer : allReviewers) {
            double denumeratorSize = (double) pullRequestDAO.findByProjectNameAndReviewerAndTimestampLessThan(project, reviewer, timestamp).size();
            for (String subProject : subProjectsArray) {
                subProjectProbabilities[index] = ((double) pullRequestDAO.findByProjectNameAndSubProjectAndReviewerAndTimestampLessThan(project, subProject, reviewer, timestamp).size() /
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
        for (Developer reviewer : allReviewers) {
            double denumeratorSize = (double) filePathDAO.findByPullRequestProjectNameAndPullRequestReviewerAndPullRequestTimestampLessThan(project, reviewer, timestamp).size();
            for (String filePath : filePathNodeOutcomes) {
                filePathNodeProbabilities[index] = //1d / filePathNodeOutcomes.length;
                        filePathDAO.findByPullRequestProjectNameAndLocationAndPullRequestReviewerAndPullRequestTimestampLessThan(project,
                                filePath, reviewer, timestamp).size() / denumeratorSize;
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
        for (Developer owner : allOwners) {
            ownerNodeOutcomes[ownerIndex] = owner.getId().toString();
            ownerIndex++;
        }
        double[] ownerNodeProbabilities = new double[allReviewers.size() * allOwners.size()];
        index = 0;
        for (Developer reviewer : allReviewers) {
            double denumeratorSize = (double) (pullRequestDAO.findByProjectNameAndReviewerAndTimestampLessThan(project, reviewer, timestamp).size());
            for (Developer owner : allOwners) {
                ownerNodeProbabilities[index] = (double) pullRequestDAO.findByProjectNameAndReviewerAndOwnerAndTimestampLessThan(project, reviewer, owner, timestamp).size() /
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
    public Map<Developer, Double> recommend(PullRequest pullRequest) {
        Map<Developer, Double> result = new HashMap<>();
        for (FilePath x : pullRequest.getFilePaths()) {
            Map<Developer, Double> resultList = recommend(pullRequest, x.getLocation());
            resultList = sortByValue(resultList);
            double y = resultList.size();
            for (Map.Entry<Developer, Double> entry : resultList.entrySet()) {
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

    private Map<Developer, Double> recommend(PullRequest pullRequest, String fileEnding) {
        Map<Developer, Double> result = new HashMap<>();

        Map<BayesNode, String> evidence = new HashMap<BayesNode, String>();

        if (new HashSet<>(allSubProjects).contains(pullRequest.getSubProject())) {
            evidence.put(subProjectNode, pullRequest.getSubProject());
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


        if (new HashSet<>(allOwners).contains(pullRequest.getOwner())) {
            evidence.put(ownerNode, pullRequest.getOwner().getId().toString());
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


    private List<Developer> findAllCodeReviewers(Long timestamp) {
        Set<Developer> allReviewers = new HashSet<>();

        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimestampLessThan(project, timestamp)) {
            for (Developer reviewer : pullRequest.getReviewer()) {
                allReviewers.add(reviewer);
            }
        }

        return new ArrayList<>(allReviewers);
    }

    public List<Developer> findAllOwners(Long timeStamp) {
        Set<Developer> allOwners = new HashSet<>();

        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimestampLessThan(project, timeStamp)) {
            allOwners.add(pullRequest.getOwner());
        }

        //add other reviewer
        Developer reviewer = new Developer();
        reviewer.setId(-1);
        allOwners.add(reviewer);

        return new ArrayList<>(allOwners);
    }

    public List<String> findAllSubProjects(Long timestamp) {
        Set<String> allSubProjects = new HashSet<>();
        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimestampLessThan(project, timestamp)) {
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

    private List<String> findAllFilePaths(long timestamp) {
        Set<String> allFilePaths = new HashSet<>();
        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimestampLessThan(project, timestamp)) {
            for (FilePath filePath : pullRequest.getFilePaths()) {
                allFilePaths.add(filePath.getLocation());
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
