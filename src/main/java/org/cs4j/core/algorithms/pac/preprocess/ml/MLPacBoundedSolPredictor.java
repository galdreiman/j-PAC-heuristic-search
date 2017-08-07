package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.apache.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.AnytimeWAStar;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.algorithms.pac.AnytimePTS4PAC;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.conditions.MLPacCondition;
import org.cs4j.core.algorithms.pac.conditions.MLPacConditionForBoundSolPredNN;
import org.cs4j.core.algorithms.pac.conditions.MLPacConditionNN;
import org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess;
import org.cs4j.core.algorithms.pac.preprocess.PacClassifierType;
import org.cs4j.core.domains.DockyardRobot;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.pac.conf.PacConfig;
import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by user on 05/08/2017.
 */
public class MLPacBoundedSolPredictor {
    private final static Logger logger = Logger.getLogger(MLPacBoundedSolPredictor.class);

    public static void main(String[] args) {


        Class[] domains = {DockyardRobot.class};// PacConfig.instance.pacPreProcessDomains();
        PacClassifierType[] clsTypes = {PacClassifierType.NN};
        double[] epsilons = {0.1}; // PacConfig.instance.inputPreprocessEpsilons();

        int numOfFeaturesPerNode = 3;

        for(Class domainClass : domains) {
            for (double epsilon : epsilons) {

                List<Integer> domainParams = MLPacHStarPredictor.domainToLevelParams.get(domainClass);
                //generate instances:
//                MLPacHStarPredictor.domainToGenerator.get(domainClass).accept(domainParams);

                int trainLevelLow = domainParams.get(0), trainLevelHigh = domainParams.get(1), trainLevelDelta = domainParams.get(2);

                int testLevel = trainLevelHigh + trainLevelDelta;

                OutputResult output = initOutputResultTable(domainClass, testLevel, trainLevelLow, trainLevelHigh, epsilon);

                for (PacClassifierType type : clsTypes) {
//                    train(domainClass, epsilon, trainLevelLow, trainLevelHigh, trainLevelDelta, numOfFeaturesPerNode, type);

                    predict(domainClass,epsilon, trainLevelLow, trainLevelHigh, testLevel, numOfFeaturesPerNode, type, output);

                }
                if (output != null) {
                    output.close();
                }

            }
        }

    }

    private static void predict(Class domainClass,double epsilon, int trainLevelLow, int trainLevelHigh, int testLevel, int numOfFeaturesPerNode, PacClassifierType classifierType,OutputResult output) {

        AbstractClassifier classifier = null;
        Instances dataset = null;

        String trainFormat = trainLevelLow + "-" + trainLevelHigh;

        String inFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
        String inputModelPath = inFile+ File.separator + "MLPacBoundedSolPreprocess_e"+epsilon+"_c_" + classifierType+"_tl_" +trainFormat+".model";
        String inputDataPath = inFile + File.separator + "MLPacBoundedSolPreprocess_e"+epsilon+"_c_" + classifierType+"_tl_" +trainFormat+".arff";
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputModelPath));
            classifier = (AbstractClassifier) ois.readObject();
            ois.close();

            dataset = MLPacPreprocess.getInputInstance(inputDataPath);
            dataset.setClassIndex(dataset.numAttributes() - 1);
        } catch (Exception e) {
            logger.error("Failed to load model for input file [" +
                    inputDataPath + "]", e);
        }





        logger.info("Running anytime for domain " + domainClass.getName());
        try {
            logger.info("Solving " + domainClass.getName());

            SearchDomain domain;
            Map<String, String> domainParams = new TreeMap<>();
            Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);

            int fromInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).fromInstance;
            int toInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).toInstance;
            String inputPath = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).pacInputPathFormat, testLevel);

            int size = 13; // calculate automatically

            WAStar optimalSolver = new WAStar();
            optimalSolver.setAdditionalParameter("weight","1.0");

            PACSearchFramework psf = new PACSearchFramework();
            psf.setAnytimeSearchClass(AnytimePTS4PAC.class);
            psf.setPACConditionClass(MLPacConditionForBoundSolPredNN.class);
            psf.setAdditionalParameter("anytimeSearch", AnytimePTS4PAC.class.getName());
            psf.setDomainLevel(trainLevelLow +"-"+ trainLevelHigh);
            psf.setAdditionalParameter("epsilon", epsilon+"");

            // go over all training set and extract features:
            for (int i = fromInstance; i <= toInstance; ++i) {
                logger.info("\rextracting features from " + domainClass.getName() + "\t instance " + i);
                domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);

                SearchResult searchResult = optimalSolver.search(domain);
                double optimalCost = searchResult.getBestSolution().getCost();

                SearchResult result = psf.search(domain);
                for(SearchResult.Solution solution: result.getSolutions()){

                    double cost = solution.getCost();

                    //--------------------------------------------
                    // extract features from all first nodes:
                    SearchDomain.Operator op;
                    AnytimeSearchNode childNode;

                    SearchDomain.State initialState = domain.initialState();
                    AnytimeSearchNode initialNode = new AnytimeSearchNode(domain, initialState);

                    SearchDomain.State unpackedInitialState = domain.unpack(initialNode.packed);

                    double initialH = initialNode.getH();

                    int indx = 0;
                    Instance ins = new DenseInstance(size);
                    if (dataset == null) {
                        return;
                    }

                    ins.setValue(new Attribute("initialH", indx++), initialH);
                    ins.setValue(new Attribute("DomainLevel", indx++), testLevel);

                    for (int opIndx = 0; opIndx < numOfFeaturesPerNode/*domain.getNumOperators(unpackedInitialState)*/; ++opIndx) {
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


                        ins.setValue(new Attribute("childH-" + opIndx, indx++), childH);
                        ins.setValue(new Attribute("childG-" + opIndx, indx++), childG);
                        ins.setValue(new Attribute("childDepth-" + opIndx, indx++), childDepth);

                        // go over gran-children:
                        for (int childOpIndx = 0; childOpIndx < numOfFeaturesPerNode /*domain.getNumOperators(childState)*/; childOpIndx++) {

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

                            ins.setValue(new Attribute("grandchildH-" + opIndx + "-" + childOpIndx, indx++), grandchildH);
                            ins.setValue(new Attribute("grandchildG-" + opIndx + "-" + childOpIndx, indx++), grandchildG);
                            ins.setValue(new Attribute("grandchildDepth-" + opIndx + "-" + childOpIndx, indx++), grandchildDepth);

                        }


                    }
                    boolean isWOpt = optimalCost * (1 + epsilon) >= cost;
                    logger.info("Optimal cost: " + optimalCost + " current cost: " + cost + " is-w-opt? " + isWOpt);

                    ins.setValue(new Attribute("is-w-opt", indx++), isWOpt+"");

                    // Classify
                    ins.setDataset(dataset);
                    double classificationResult = -1;
                    if (classifier == null) {
                        return;
                    }
                    try {
                        logger.info(ins.toString());
                        classificationResult = classifier.classifyInstance(ins);
                    } catch (Exception e) {
                        logger.error("ERROR: Failed to classify instance: ", e);
                    }

                    logger.info(classificationResult);

                    output.writeln(i + "," + classificationResult + "," + optimalCost + "," + trainLevelLow + "," + trainLevelHigh + "," + testLevel + "," + classifier.getClass().getSimpleName() + "," + domainClass.getSimpleName());

                }
            }

        } catch (Exception e) {
            logger.error(e);
        }


    }


    private static void train(Class domainClass,double epsilon, int trainLevelLow, int trainLevelHigh, int trainLevelDelta,int numOfFeaturesPerNode, PacClassifierType classifierType) {
        String outfilePostfix = ".arff";

        OutputResult output = null;
        OutputResult csvOutput = null;
        String trainFormat = trainLevelLow + "-" + trainLevelHigh;
        String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
        try {
            output = new OutputResult(outFile, "MLPacBoundedSolPreprocess_e"+epsilon+"_c_" + classifierType+"_tl_" +trainFormat, true,outfilePostfix);
            csvOutput = new OutputResult(outFile, "MLPacBoundedSolPreprocess_e" +epsilon+"_c_"+ classifierType+"_tl_" +trainFormat, true,".csv");
        } catch (IOException e1) {
            logger.error("Failed to create output ML PAC preprocess output file at: " + outFile, e1);
        }

        String tableHeader = MLPacFeatureExtractor.getFeaturesARFFHeaderBoundSolPred();
        String csvTableHeader = MLPacFeatureExtractor.getFeaturesCsvHeaderBoundSolPred();
        try {
            output.writeln(tableHeader);
            csvOutput.writeln(csvTableHeader);
        } catch (IOException e1) {
            logger.error("Failed to write header to output ML preprocess table: " + tableHeader, e1);
        }

        logger.info("Running anytime for domain " + domainClass.getName());
        try {

            for(int trainLevel = trainLevelLow; trainLevel <= trainLevelHigh; trainLevel += trainLevelDelta) {
                String tableForDomainLevel = extractTableOfFeatures(domainClass, trainLevel, epsilon);

                output.writeln(tableForDomainLevel.toString());
                csvOutput.writeln(tableForDomainLevel.toString());
            }
            output.close();
            csvOutput.close();

            // -------------------------------------------------
            // 3. train + save the model to file
            // -------------------------------------------------
            AbstractClassifier cls =
                    MLPacPreprocess.setupAndGetClassifier(output.getFname(), classifierType,false,outFile+ File.separator + "MLPacBoundedSolPreprocess_"+classifierType+"_"+trainFormat+".arff");
            String outputModel = outFile+ File.separator + "MLPacBoundedSolPreprocess_e"+epsilon+"_c_"+classifierType+"_tl_"+trainFormat+".model";
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputModel));
            oos.writeObject(cls);
            oos.flush();




        } catch (Exception e) {
            logger.error(e);
        } finally {
            if (output != null) {
                output.close();
                csvOutput.close();
            }
        }

    }

    private static String extractTableOfFeatures(Class domainClass, int trainLevel, double epsilon){
        logger.info("Solving " + domainClass.getName());


        SearchDomain domain;
        Map<String, String> domainParams = new TreeMap<>();
        Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);

        int fromInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).fromInstance;
        int toInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).toInstance;
        String inputPath = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).pacInputPathFormat, trainLevel);

        AnytimeSearchAlgorithm solver = MLPacPreprocess.getAnytimeAlg(epsilon);

        WAStar optimalSolver = new WAStar();
        optimalSolver.setAdditionalParameter("weight","1.0");

        StringBuilder sb = new StringBuilder();

        // go over all training set and extract features:
        for (int i = fromInstance; i <= toInstance; ++i) {
            logger.info("\rextracting features from " + domainClass.getName() + "\t instance " + i);
            domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);

            // get optimal solution:
            SearchResult optSearchResult = optimalSolver.search(domain);
            double optimalCost = optSearchResult.getBestSolution().getCost();

            // get anytime solution:
            SearchResult searchResult = solver.search(domain);
            while (searchResult.hasSolution()){


                double cost = searchResult.getBestSolution().getCost();

                Map<MLPacFeatureExtractor.PacFeature, Double> features = MLPacFeatureExtractor
                        .extractFeaturesFromSearchResultIncludeTarget(searchResult, optimalCost, epsilon);

                double generated = features.get(MLPacFeatureExtractor.PacFeature.GENERATED);
                double expanded = features.get(MLPacFeatureExtractor.PacFeature.EXPANDED);
                double reopened = features.get(MLPacFeatureExtractor.PacFeature.ROPENED);
                double U = features.get(MLPacFeatureExtractor.PacFeature.COST);

                double g1 = features.get(MLPacFeatureExtractor.PacFeature.G_0);
                double h1 = features.get(MLPacFeatureExtractor.PacFeature.H_0);

                double g2 = features.get(MLPacFeatureExtractor.PacFeature.G_2);
                double h2 = features.get(MLPacFeatureExtractor.PacFeature.H_2);

                double g3 = features.get(MLPacFeatureExtractor.PacFeature.G_2);
                double h3 = features.get(MLPacFeatureExtractor.PacFeature.H_2);

                double w = 1.0 + (Double) searchResult.getExtras().get("epsilon");

                boolean isWOptimal =  optimalCost * (1 + epsilon) >= cost;

                String[] lineParts = {generated+"",expanded+"",reopened+"", trainLevel+"",U+"",g1+"",h1+"",g2+"",h2+"",g3+"",h3+"",w+"",isWOptimal+"\n"};
                String line = String.join(",", lineParts);
                sb.append(line);

                searchResult = solver.continueSearch();

            }
        }

        return sb.toString();
    }

    private static OutputResult initOutputResultTable(Class domainClass, int testLevel, int trainLevelLow, int trainLevelHigh, double epsilon) {

        OutputResult output = null;
        String trainFormat = trainLevelLow + "-" + trainLevelHigh;
        String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
        try {
            output = new OutputResult(outFile, "MLPacBoundedSolPredictionResults_e" +epsilon +"_tl_"+ testLevel, true, ".csv");
        } catch (IOException e1) {
            logger.error("Failed to create output ML PAC preprocess output file at: " + outFile, e1);
        }

        String tableHeader = "instance_id, h*_prediction, h*_actual, trainLevelLow, trainLevelHigh, testLevel, classifier, domain";
        try {
            output.writeln(tableHeader);
        } catch (IOException e1) {
            logger.error("Failed to write header to output ML preprocess table: " + tableHeader, e1);
        }
        return output;
    }
}
