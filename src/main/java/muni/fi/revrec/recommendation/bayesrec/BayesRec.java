package muni.fi.revrec.recommendation.bayesrec;

import muni.fi.revrec.common.exception.ReviewerRecommendationException;
import muni.fi.revrec.model.filePath.FilePath;
import muni.fi.revrec.model.filePath.FilePathDAO;
import muni.fi.revrec.model.pullRequest.PullRequest;
import muni.fi.revrec.model.pullRequest.PullRequestDAO;
import muni.fi.revrec.model.reviewer.Developer;
import muni.fi.revrec.recommendation.ReviewerRecommendation;
import muni.fi.revrec.recommendation.ReviewerRecommendationBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * Implementation of reviewer recommendation with the usage of Naive Bayes.
 *
 * @author Jakub Lipcak, Masaryk University
 */
@Service
public class BayesRec extends ReviewerRecommendationBase implements ReviewerRecommendation {

    private static final double SMOOTHING_VARIABLE = 0.01;
    private final Log logger = LogFactory.getLog(this.getClass());

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
                    @Value("${recommendation.retired.remove}") boolean removeRetiredReviewers,
                    @Value("${recommendation.retired.interval}") long timeRetiredInMonths,
                    @Value("${recommendation.project}") String project) {
        super(pullRequestDAO, removeRetiredReviewers, timeRetiredInMonths, project);
        this.filePathDAO = filePathDAO;
    }

    @Override
    public void buildModel() {
        buildModel(Long.MAX_VALUE);
    }

    public void buildModel(Long timestamp) {
        logger.info("Building probabilistic model...");

        allFilePaths = findAllFilePaths(timestamp);
        allOwners = findAllOwners(timestamp);
        allReviewers = findAllCodeReviewers(timestamp);
        allSubProjects = findAllSubProjects(timestamp);
        int index = 0;

        //Build Bayesian network
        BayesNet net = new BayesNet();


        //Reviewers node
        logger.info("Computing probabilities of reviewers...");

        BayesNode reviewersNode = net.createNode("reviewersNode");
        String reviewersOutcomes[] = new String[allReviewers.size()];
        double[] reviewersProbabilities = new double[allReviewers.size()];
        double allReviewersSize = (double) pullRequestDAO.findByProjectNameAndTimestampLessThan(project, timestamp).size();
        for (Developer reviewer : allReviewers) {
            reviewersOutcomes[index] = reviewer.getId().toString();
            reviewersProbabilities[index] = ((double) (pullRequestDAO.findByReviewerAndProjectNameAndTimestampLessThan(reviewer, project, timestamp).size()) / allReviewersSize);
            index++;
            logger.info(index + "/" + reviewersProbabilities.length);
        }
        reviewersNode.addOutcomes(reviewersOutcomes);
        reviewersNode.setProbabilities(reviewersProbabilities);

        //Subproject node
        logger.info("Computing probabilities of subprojects...");

        BayesNode subProjectNode = net.createNode("subProjectNode");
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
                logger.info(index + "/" + subProjectProbabilities.length);
            }
            subProjectProbabilities = laplaceSmoothing(index - subProjectsArray.length, index, subProjectProbabilities);
        }

        subProjectNode.addOutcomes(subProjectOutcomes);
        subProjectNode.setParents(Arrays.asList(reviewersNode));
        subProjectNode.setProbabilities(subProjectProbabilities);


        //File path node
        logger.info("Computing probabilities of file paths...");

        BayesNode filePathNode = net.createNode("filePathNode");
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
                logger.info(index + "/" + filePathNodeProbabilities.length);
            }
            filePathNodeProbabilities = laplaceSmoothing(index - filePathNodeOutcomes.length, index, filePathNodeProbabilities);
        }
        filePathNode.addOutcomes(filePathNodeOutcomes);
        filePathNode.setParents(Arrays.asList(reviewersNode));
        filePathNode.setProbabilities(filePathNodeProbabilities);


        //Owner node
        logger.info("Computing probabilities of pull request owners...");

        BayesNode ownerNode = net.createNode("ownerNode");
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
                logger.info(index + "/" + ownerNodeProbabilities.length);
            }
            ownerNodeProbabilities = laplaceSmoothing(index - allOwners.size(), index, ownerNodeProbabilities);
        }

        ownerNode.addOutcomes(ownerNodeOutcomes);
        ownerNode.setParents(Arrays.asList(reviewersNode));
        ownerNode.setProbabilities(ownerNodeProbabilities);


        inferer = new JunctionTreeAlgorithm();
        inferer.setNetwork(net);
        this.reviewersNode = reviewersNode;
        this.subProjectNode = subProjectNode;
        this.filePathNode = filePathNode;
        this.ownerNode = ownerNode;
    }

    @Override
    public List<Developer> recommend(PullRequest pullRequest) {
        if (inferer == null) {
            throw new ReviewerRecommendationException("Model is not built yet!");
        }

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
        return processResult(result, pullRequest);
    }

    private Map<Developer, Double> recommend(PullRequest pullRequest, String filePathLocation) {
        Map<Developer, Double> result = new HashMap<>();

        Map<BayesNode, String> evidence = new HashMap<BayesNode, String>();

        if (new HashSet<>(allSubProjects).contains(pullRequest.getSubProject())) {
            evidence.put(subProjectNode, pullRequest.getSubProject());
        } else {
            System.out.println("Bayes otherSubProject");
            evidence.put(subProjectNode, "otherSubProject");
        }

        if (new HashSet<>(allFilePaths).contains(filePathLocation)) {
            evidence.put(filePathNode, filePathLocation);
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
            logger.error(ex.getMessage());
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
}
