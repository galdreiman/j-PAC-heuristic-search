package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.apache.log4j.Logger;
import org.cs4j.core.AnytimeSearchAlgorithm;
import org.cs4j.core.MLPacFeatureExtractor;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.algorithms.pac.SearchAwarePACSearchImpl;
import org.cs4j.core.algorithms.pac.conditions.MLPacCondition;
import org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.experiments.Experiment;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.experiments.MLPacExperiment;
import org.cs4j.core.experiments.PACOnlineExperimentRunner;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.pac.conf.PacConfig;
import weka.classifiers.AbstractClassifier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess.ExtractFeaturesToFile;

/**
 * Created by user on 22/05/2017.
 */
public class MLPacStatisticsPredictor {
    private final static Logger logger = Logger.getLogger(MLPacPreprocess.class);
    private static OutputResult output;

    public static void main(String[] args){


        double[] inputEpsilon = PacConfig.instance.inputPreprocessEpsilons();
        Class[] domains = {Pancakes.class}; //PacConfig.instance.pacPreProcessDomains();//{  VacuumRobot.class};//, VacuumRobot.class,  Pancakes.class};


        String outfilePostfix = ".csv";

        for (Class domainClass : domains) {
                int trainLevel = 10;
                int testLevel = 15;

                String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainLevel);
                try {
                    output = new OutputResult(outFile, "MLPacStatsPreprocess", true,outfilePostfix);
                } catch (IOException e1) {
                    logger.error("Failed to create output ML PAC preprocess output file at: " + outFile, e1);
                }

                String tableHeader = MLPacFeatureExtractor.getFeaturesARFFHeaderForPredictedStats();
                try {
                    output.writeln(tableHeader);
                } catch (IOException e1) {
                    logger.error("Failed to write header to output ML preprocess table: " + tableHeader, e1);
                }

                logger.info("Running anytime for domain " + domainClass.getName());
                try {
                    logger.info("Solving " + domainClass.getName());

                    // -------------------------------------------------
                    // 1. load PAC statistics (to get optimal solutions
                    // -------------------------------------------------

                    PACUtils.getPACStatistics(domainClass,DomainExperimentData.RunType.ALL,trainLevel);

                    // --------------------------------------------------------------
                    // 2. for each problem from train: extract features
                    // --------------------------------------------------------------

                    SearchDomain domain;
                    Map<String, String> domainParams = new TreeMap<>();
                    Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);

                    int fromInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).fromInstance;
                    int toInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).toInstance;
                    String inputPath = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).pacInputPathFormat, trainLevel);

                    WAStar optimalSolver = new WAStar();
                    optimalSolver.setAdditionalParameter("weight","1.0");

                    // go over all training set and extract features:
                    for (int i = fromInstance; i <= toInstance; ++i) {
                        logger.info("\rextracting features from " + domainClass.getName() + "\t instance " + i);
                        domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);

                        double optimalCost = PACUtils.getOptimalSolution(domainClass, i);
                        double initialH = domain.initialState().getH();

                        //--------------------------------------------
                        // extract features from all first nodes:
                        SearchDomain.Operator op;
                        AnytimeSearchNode childNode;

                        SearchDomain.State initialState = domain.initialState();
                        AnytimeSearchNode initialNode = new AnytimeSearchNode(domain,initialState);

                        SearchDomain.State unpackedInitialState = domain.unpack(initialNode.packed);
                        StringBuilder sb = new StringBuilder();

                        for (int opIndx = 0; opIndx < domain.getNumOperators(unpackedInitialState); ++opIndx) {
                            // Get the current operator
                            op = domain.getOperator(unpackedInitialState, opIndx);
                            // Don't apply the previous operator on the state - in order not to enter a loop
                            if (op.equals(initialNode.pop)) {
                                continue;
                            }
                            // Get it by applying the operator on the parent state
                            SearchDomain.State childState = domain.applyOperator(initialState, op);
                            // Create a search node for this state
                            childNode = new AnytimeSearchNode(domain,
                                    childState,
                                    initialNode,
                                    initialState,
                                    op, op.reverse(initialState));

                            double childH = childNode.getH();
                            double childG = childNode.getG();
                            double childDepth = childNode.getDepth();

                            sb.append(optimalCost);
                            sb.append(" ");
                            sb.append(childH);
                            sb.append(" ");
                            sb.append(childG);
                            sb.append(" ");
                            sb.append(childDepth);
                            sb.append(" ");

                            // go over gran-children:
                            for(int childOpIndx = 0; childOpIndx < domain.getNumOperators(childState); childOpIndx++) {

                                op = domain.getOperator(childState, opIndx);
                                SearchDomain.State grandchildState = domain.applyOperator(childState, op);
                                // Create a search node for this state
                                AnytimeSearchNode grandchildNode = new AnytimeSearchNode(domain,
                                        grandchildState,
                                        childNode,
                                        childState,
                                        op, op.reverse(childState));

                                double grandchildH = grandchildNode.getH();
                                double grandchildG = grandchildNode.getG();
                                double grandchildDepth = grandchildNode.getDepth();

                                sb.append(grandchildH);
                                sb.append(" ");
                                sb.append(grandchildG);
                                sb.append(" ");
                                sb.append(grandchildDepth);
                                sb.append(" ");
                            }


                        }
                        output.writeln(sb.toString().replace(" ", ","));


                    }
                    output.close();

                    // -------------------------------------------------
                    // 3. save the model to file
                    // -------------------------------------------------

                    // -------------------------------------------------
                    // 4. train a model
                    // -------------------------------------------------




                } catch (Exception e) {
                    logger.error(e);
                } finally {
                    if (output != null) {
                        output.close();
                    }
                }
        }

    }
}
