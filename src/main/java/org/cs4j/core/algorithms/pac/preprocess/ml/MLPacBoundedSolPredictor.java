package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.apache.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.algorithms.pac.AnytimePTS4PAC;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.conditions.MLPacConditionForBoundSolPredNN;
import org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess;
import org.cs4j.core.algorithms.pac.preprocess.PacClassifierType;
import org.cs4j.core.domains.DockyardRobot;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.pac.conf.PacConfig;
import weka.classifiers.AbstractClassifier;
import weka.core.Instances;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Gal Dreiman on 05/08/2017.
 */
public class MLPacBoundedSolPredictor {
    private final static Logger logger = Logger.getLogger(MLPacBoundedSolPredictor.class);

    public static void main(String[] args) {


        Class[] domains =  PacConfig.instance.pacDomains();
        PacClassifierType[] clsTypes = {PacClassifierType.NN};
        double[] epsilons = PacConfig.instance.inputPreprocessEpsilons();


        for(Class domainClass : domains) {
            for (double epsilon : epsilons) {

                List<Integer> domainParams = MLPacHStarPredictor.domainToLevelParams.get(domainClass);
                //generate instances:
                MLPacHStarPredictor.domainToGenerator.get(domainClass).accept(domainParams);

                int trainLevelLow = domainParams.get(0), trainLevelHigh = domainParams.get(1), trainLevelDelta = domainParams.get(2);

                int testLevel = trainLevelHigh + trainLevelDelta;

                OutputResult output = initOutputResultTable(domainClass, testLevel, trainLevelLow, trainLevelHigh, epsilon);

                for (PacClassifierType type : clsTypes) {
                    train(domainClass, epsilon, trainLevelLow, trainLevelHigh, trainLevelDelta, type);

                    predict(domainClass,epsilon, trainLevelLow, trainLevelHigh, testLevel, type, output);

                }
                if (output != null) {
                    output.close();
                }

            }
        }

    }

    private static void predict(Class domainClass,double epsilon, int trainLevelLow, int trainLevelHigh, int testLevel, PacClassifierType classifierType,OutputResult output) {

        AbstractClassifier classifier = null;
        Instances dataset = null;

        double[] deltas = PacConfig.instance.inputOnlineDeltas();
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

            for(double delta : deltas) {
                PACSearchFramework psf = new PACSearchFramework();
                psf.setAnytimeSearchClass(AnytimePTS4PAC.class);
                psf.setPACConditionClass(MLPacConditionForBoundSolPredNN.class);
                psf.setAdditionalParameter("anytimeSearch", AnytimePTS4PAC.class.getName());
                psf.setDomainLevel(trainLevelLow + "-" + trainLevelHigh);
                psf.setAdditionalParameter("epsilon", epsilon + "");
                psf.setAdditionalParameter("delta", delta + "");

                // go over all training set and extract features:
                for (int i = fromInstance; i <= toInstance; ++i) {
                    logger.info("\rextracting features from " + domainClass.getName() + "\t instance " + i);
                    domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);

                    SearchResult searchResult = optimalSolver.search(domain);
                    double optimalCost = searchResult.getBestSolution().getCost();

                    SearchResult result = psf.search(domain);

                    SearchResult.Solution bestSolution = result.getBestSolution();
                    bestSolution.getCost();
                    bestSolution.getStates();


                    String instanceID = i + ",";
                    String found = result.hasSolution() ? "1," : "0,";
                    String depth = bestSolution.getOperators().size() + ",";
                    String cost = bestSolution.getCost() + ",";
                    String iterations = result.getSolutions().size() + ",";
                    String generated = result.getGenerated() + ",";
                    String expanded = result.getExpanded() + ",";
                    String cpuTime = result.getCpuTimeMillis() + ",";
                    String wallTime = result.getWallTimeMillis() + ",";
                    String Delta = delta + ",";
                    String Epsilon = epsilon + ",";
                    String TrainLevelLow = trainLevelLow + ",";
                    String TrainLevelHigh = trainLevelHigh + ",";
                    String TestLevel = testLevel + ",";
                    String pacCondition = classifier.getClass().getSimpleName() + ",";
                    String Domain = domainClass.getSimpleName() + ",";
                    String OPT = optimalCost + ",";
                    String isEpsilon = optimalCost * (1 + epsilon) >= bestSolution.getCost() == true ? "1" : "0";

                    // InstanceID	Found	Depth	Cost	Iterations	Generated	Expanded	Cpu Time	Wall Time	delta	epsilon TrainLevelLow TrainLevelHigh  TestLevel	pacCondition	Domain	OPT	is_epsilon
                    output.writeln(instanceID + found + depth + cost + iterations + generated + expanded + cpuTime + wallTime + Delta + Epsilon + TrainLevelLow + TrainLevelHigh + TestLevel + pacCondition + Domain + OPT + isEpsilon);
                }
            }

        } catch (Exception e) {
        }


    }


    private static void train(Class domainClass,double epsilon, int trainLevelLow, int trainLevelHigh, int trainLevelDelta, PacClassifierType classifierType) {
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

        //instanceID+found+depth+cost+iterations+generated+expanded+cpuTime+wallTime+delta+Epsilon+TrainLevelLow+TrainLevelHigh+TestLevel+pacCondition+Domain+OPT+isEpsilon
        String tableHeader = "InstanceID,Found,Depth,Cost,Iterations,generated,Expanded,Cpu-Time,Wall-Time,delta,epsilon,TrainLevelLow,TrainLevelHigh,TestLevel,pacCondition,Domain,OPT,is_epsilon";
        try {
            output.writeln(tableHeader);
        } catch (IOException e1) {
            logger.error("Failed to write header to output ML preprocess table: " + tableHeader, e1);
        }
        return output;
    }
}
