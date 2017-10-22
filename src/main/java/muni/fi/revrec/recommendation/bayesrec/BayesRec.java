package muni.fi.revrec.recommendation.bayesrec;

import muni.fi.revrec.common.exception.ReviewerRecommendationException;
import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.filePath.FilePathDAO;
import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.pullRequest.PullRequestDAO;
import muni.fi.revrec.model.reviewer.Developer;
import muni.fi.revrec.recommendation.ReviewerRecommendation;
import muni.fi.revrec.recommendation.ReviewerRecommendationBase;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.inference.jtree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.util.NumericalInstabilityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Implementation of Naive Bayes-based Code Reviewers Recommendation Algorithm.
 *
 * @author Jakub Lipcak, Masaryk University
 */
@Service
public class BayesRec extends ReviewerRecommendationBase implements ReviewerRecommendation {

    private static final double SMOOTHING_VARIABLE = 0.01;

    @Autowired
    private FilePathDAO filePathDAO;

    //Bayes network attributes
    private IBayesInferer inferer;
    private BayesNode reviewersNode;
    private BayesNode subProjectNode;
    private BayesNode filePathNode;
    private BayesNode ownerNode;

    private List<Developer> allReviewers;
    private List<String> allFilePaths;
    private List<Developer> allOwners;
    private List<String> allSubProjects;

    public BayesRec(@Autowired PullRequestDAO pullRequestDAO,
                    @Autowired FilePathDAO filePathDAO,
                    @Value("${recommendation.retired}") boolean removeRetiredReviewers,
                    @Value("${recommendation.retired.interval}") long timeRetiredInMonths,
                    @Value("${recommendation.project}") String project) {
        super(pullRequestDAO, removeRetiredReviewers, timeRetiredInMonths, project);
        this.filePathDAO = filePathDAO;
    }

    @Override
    public void buildModel() {
        buildModel(Long.MAX_VALUE);
    }

    /**
     * Build model from pull requests, which were created before timestamp.
     *
     * @param timestamp model is built from pull requests, which were created earlier than timestamp.
     */
    public void buildModel(Long timestamp) {
        logger.info("Building probabilistic model...");

        allFilePaths = findAllFilePaths(timestamp);
        allOwners = findAllOwners(timestamp);
        allReviewers = findAllCodeReviewers(timestamp);
        allSubProjects = findAllSubProjects(timestamp);

        //Build Bayesian network
        BayesNet net = new BayesNet();

        //Build Reviewers node
        BayesNode reviewersNode = createReviewersNode(net, timestamp);

        //Build Subproject node
        BayesNode subProjectNode = createSubProjectsNode(net, timestamp, reviewersNode);

        //Build File path node
        BayesNode filePathNode = createFilePathsNode(net, timestamp, reviewersNode);

        //Build Owner node
        BayesNode ownerNode = createOwnersNode(net, timestamp, reviewersNode);


        inferer = new JunctionTreeAlgorithm();
        inferer.setNetwork(net);
        this.reviewersNode = reviewersNode;
        this.subProjectNode = subProjectNode;
        this.filePathNode = filePathNode;
        this.ownerNode = ownerNode;
    }

    /**
     * Create Reviewers node of Naive Bayes.
     *
     * @param net       Bayesian network.
     * @param timestamp we only consider records created before timestamp.
     * @return node of Naive Bayes.
     */
    private BayesNode createReviewersNode(BayesNet net, Long timestamp) {
        logger.info("Computing probabilities of reviewers...");

        int index = 0;
        BayesNode reviewersNode = net.createNode("reviewersNode");
        String reviewersOutcomes[] = new String[allReviewers.size()];
        double[] reviewersProbabilities = new double[allReviewers.size()];
        double allReviewersSize = (double) pullRequestDAO.findByProjectNameAndTimestampLessThan(project, timestamp).size();
        for (Developer reviewer : allReviewers) {
            reviewersOutcomes[index] = reviewer.getId().toString();
            reviewersProbabilities[index] = ((double) (pullRequestDAO.countByReviewersAndProjectNameAndTimestampLessThan(reviewer, project, timestamp)) / allReviewersSize);
            index++;
            logger.info(index + "/" + reviewersProbabilities.length);
        }
        reviewersNode.addOutcomes(reviewersOutcomes);
        reviewersNode.setProbabilities(reviewersProbabilities);

        return reviewersNode;
    }

    /**
     * Create Sub-projects node of Naive Bayes.
     *
     * @param net       Bayesian network.
     * @param timestamp we only consider records created before timestamp.
     * @return node of Naive Bayes.
     */
    private BayesNode createSubProjectsNode(BayesNet net, Long timestamp, BayesNode parentNode) {
        logger.info("Computing probabilities of subprojects...");

        int index = 0;
        BayesNode subProjectNode = net.createNode("subProjectNode");
        String subProjectOutcomes[] = new String[allSubProjects.size()];
        double[] subProjectProbabilities = new double[allReviewers.size() * allSubProjects.size()];
        String[] subProjectsArray = allSubProjects.toArray(new String[allSubProjects.size()]);
        int subProjectIndex = 0;
        for (String subProject : subProjectsArray) {
            subProjectOutcomes[subProjectIndex] = subProject;
            subProjectIndex++;
        }

        for (Developer reviewer : allReviewers) {
            double denumeratorSize = (double) pullRequestDAO.findByProjectNameAndReviewersAndTimestampLessThan(project, reviewer, timestamp).size();
            for (String subProject : subProjectsArray) {
                subProjectProbabilities[index] = ((double) pullRequestDAO.countByProjectNameAndSubProjectAndReviewersAndTimestampLessThan(project, subProject, reviewer, timestamp) /
                        denumeratorSize);
                index++;
                logger.info(index + "/" + subProjectProbabilities.length);
            }
            subProjectProbabilities = laplaceSmoothing(index - subProjectsArray.length, index, subProjectProbabilities);
        }

        subProjectNode.addOutcomes(subProjectOutcomes);
        subProjectNode.setParents(Arrays.asList(parentNode));
        subProjectNode.setProbabilities(subProjectProbabilities);

        return subProjectNode;
    }

    /**
     * Create Owners node of Naive Bayes.
     *
     * @param net       Bayesian network.
     * @param timestamp we only consider records created before timestamp.
     * @return node of Naive Bayes.
     */
    private BayesNode createOwnersNode(BayesNet net, Long timestamp, BayesNode parentNode) {
        logger.info("Computing probabilities of pull request owners...");

        int index = 0;
        BayesNode ownerNode = net.createNode("ownerNode");
        String[] ownerNodeOutcomes = new String[allOwners.size()];
        int ownerIndex = 0;
        for (Developer owner : allOwners) {
            ownerNodeOutcomes[ownerIndex] = owner.getId().toString();
            ownerIndex++;
        }
        double[] ownerNodeProbabilities = new double[allReviewers.size() * allOwners.size()];
        for (Developer reviewer : allReviewers) {
            double denumeratorSize = (double) (pullRequestDAO.findByProjectNameAndReviewersAndTimestampLessThan(project, reviewer, timestamp).size());
            for (Developer owner : allOwners) {
                ownerNodeProbabilities[index] = ((double) pullRequestDAO.countByProjectNameAndReviewersAndOwnerAndTimestampLessThan(project,
                        reviewer, owner, timestamp)) / denumeratorSize;
                index++;
                logger.info(index + "/" + ownerNodeProbabilities.length);
            }
            ownerNodeProbabilities = laplaceSmoothing(index - allOwners.size(), index, ownerNodeProbabilities);
        }

        ownerNode.addOutcomes(ownerNodeOutcomes);
        ownerNode.setParents(Arrays.asList(parentNode));
        ownerNode.setProbabilities(ownerNodeProbabilities);

        return ownerNode;
    }

    /**
     * Create File paths node of Naive Bayes.
     *
     * @param net       Bayesian network.
     * @param timestamp we only consider records created before timestamp.
     * @return node of Naive Bayes.
     */
    private BayesNode createFilePathsNode(BayesNet net, Long timestamp, BayesNode parentNode) {
        int index = 0;

        logger.info("Computing probabilities of file paths...");
        BayesNode filePathNode = net.createNode("filePathNode");
        String[] filePathNodeOutcomes = allFilePaths.toArray(new String[allFilePaths.size()]);
        double[] filePathNodeProbabilities = new double[allReviewers.size() * allFilePaths.size()];
        for (Developer reviewer : allReviewers) {
            double denumeratorSize = (double) filePathDAO.findByPullRequestProjectNameAndPullRequestReviewersAndPullRequestTimestampLessThan(project, reviewer, timestamp).size();
            for (String filePath : filePathNodeOutcomes) {
                filePathNodeProbabilities[index] =
                        ((double) filePathDAO.countByPullRequestProjectNameAndLocationAndPullRequestReviewersAndPullRequestTimestampLessThan(project,
                                filePath, reviewer, timestamp)) / denumeratorSize;
                index++;
                logger.info(index + "/" + filePathNodeProbabilities.length);
            }
            filePathNodeProbabilities = laplaceSmoothing(index - filePathNodeOutcomes.length, index, filePathNodeProbabilities);
        }
        filePathNode.addOutcomes(filePathNodeOutcomes);
        filePathNode.setParents(Arrays.asList(parentNode));
        filePathNode.setProbabilities(filePathNodeProbabilities);

        return filePathNode;
    }

    @Override
    public List<Developer> recommend(PullRequest pullRequest) {
        if (inferer == null) {
            throw new ReviewerRecommendationException("Model is not built yet!");
        }

        Map<Developer, Double> result = new HashMap<>();

        //recommendation is done for every file path separately
        for (FilePath x : pullRequest.getFilePaths()) {
            Map<Developer, Double> resultList = recommend(pullRequest, x.getLocation());
            resultList = sortByValue(resultList);
            double y = resultList.size();

            //assign points to code reviewers
            for (Map.Entry<Developer, Double> entry : resultList.entrySet()) {
                if (result.containsKey(entry.getKey())) {
                    result.replace(entry.getKey(), result.get(entry.getKey()) + y);
                } else {
                    result.put(entry.getKey(), y);
                }
                y--;
            }
        }

        return processResult(result, pullRequest);
    }

    /**
     * Recommend reviewers for one file path.
     *
     * @param pullRequest      pullRequest, for which reviewers are recommended.
     * @param filePathLocation file path processed as the feature.
     * @return map of reviewers and their points assigned by the recommendation algorithm.
     */
    private Map<Developer, Double> recommend(PullRequest pullRequest, String filePathLocation) {
        Map<Developer, Double> result = new HashMap<>();
        Map<BayesNode, String> evidence = new HashMap<>();

        if (new HashSet<>(allSubProjects).contains(pullRequest.getSubProject())) {
            evidence.put(subProjectNode, pullRequest.getSubProject());
        } else {
            //unknown variable
            evidence.put(subProjectNode, "otherSubProject");
        }

        if (new HashSet<>(allFilePaths).contains(filePathLocation)) {
            evidence.put(filePathNode, filePathLocation);
        } else {
            //unknown variable
            evidence.put(filePathNode, "otherFilePath");
        }


        if (new HashSet<>(allOwners).contains(pullRequest.getOwner())) {
            evidence.put(ownerNode, pullRequest.getOwner().getId().toString());
        } else {
            //unknown variable (represented by owner with id=-1)
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
            logger.error(ex.getMessage());
            return result;
        }
    }


    /**
     * Find all code reviewers of pull requests, who reviewed at least one pull request earlier than timestamp.
     *
     * @param timestamp time specification.
     * @return all code reviewers, who reviewed at least one pull request earlier than timestamp.
     */
    private List<Developer> findAllCodeReviewers(Long timestamp) {
        Set<Developer> allReviewers = new HashSet<>();

        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimestampLessThan(project, timestamp)) {
            allReviewers.addAll(pullRequest.getReviewers());
        }

        return new ArrayList<>(allReviewers);
    }

    /**
     * Find all sub-projects of pull requests created earlier than timestamp.
     *
     * @param timestamp time specification.
     * @return all sub-projects of pull requests created before timestamp.
     */
    public List<Developer> findAllOwners(Long timestamp) {
        Set<Developer> allOwners = new HashSet<>();

        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimestampLessThan(project, timestamp)) {
            allOwners.add(pullRequest.getOwner());
        }

        //add other owner representing an unknown variable (with id=-1)
        Developer reviewer = new Developer();
        reviewer.setId(-1);
        allOwners.add(reviewer);

        return new ArrayList<>(allOwners);
    }

    /**
     * Find all sub-projects of pull requests created earlier than timestamp.
     *
     * @param timestamp time specification.
     * @return all sub-projects of pull requests created before timestamp.
     */
    public List<String> findAllSubProjects(Long timestamp) {
        Set<String> allSubProjects = new HashSet<>();
        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimestampLessThan(project, timestamp)) {
            allSubProjects.add(pullRequest.getSubProject());
        }

        //add otherSubProject representing an unknown variable
        allSubProjects.add("otherSubProject");

        return new ArrayList<>(allSubProjects);
    }

    /**
     * Find all file paths of pull requests created earlier than timestamp.
     *
     * @param timestamp time specification.
     * @return all file paths of pull requests created before timestamp.
     */
    private List<String> findAllFilePaths(long timestamp) {
        Set<String> allFilePaths = new HashSet<>();
        for (PullRequest pullRequest : pullRequestDAO.findByProjectNameAndTimestampLessThan(project, timestamp)) {
            for (FilePath filePath : pullRequest.getFilePaths()) {
                allFilePaths.add(filePath.getLocation());
            }
        }

        //add otherFilePath representing an unknown variable
        allFilePaths.add("otherFilePath");

        return new ArrayList<>(allFilePaths);
    }


    /**
     * Distribute the value of SMOOTHING_VARIABLE to all elements with zero probability
     * between beginIndex and endIndex in the array. The value of SMOOTHING_VARIABLE will
     * be equally subtracted from elements with non-zero probability between these indexes.
     *
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex   the ending index, exclusive.
     * @param array      probability array.
     * @return array modified by SMOOTHING_VARIABLE.
     */
    private double[] laplaceSmoothing(int beginIndex, int endIndex, double[] array) {
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
                array[x] = array[x] - (SMOOTHING_VARIABLE / nonZeroElements);
            } else {
                array[x] = array[x] + (SMOOTHING_VARIABLE / zeroElements);
            }
        }

        return array;
    }
}
