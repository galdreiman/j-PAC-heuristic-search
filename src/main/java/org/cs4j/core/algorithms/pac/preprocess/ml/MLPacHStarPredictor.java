package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.apache.log4j.Logger;
import org.cs4j.core.MLPacFeatureExtractor;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess;
import org.cs4j.core.algorithms.pac.preprocess.PacClassifierType;
import org.cs4j.core.domains.DockyardRobot;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.domains.VacuumRobot;
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
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by Gal Dreiman on 22/05/2017.
 */
public class MLPacHStarPredictor {
    private final static Logger logger = Logger.getLogger(MLPacHStarPredictor.class);

    protected static Map<Class, Consumer<List<Integer>>> domainToGenerator;
    static{
        domainToGenerator = new HashMap<>();
        domainToGenerator.put(DockyardRobot.class,(lst) -> {
            try {
                DockyardRobotGeneratorForMLPac.generate(lst.get(0), lst.get(1) + lst.get(2), lst.get(2));
            } catch(Exception e){
                logger.error(e);
            }
        });
        domainToGenerator.put(VacuumRobot.class,(lst) -> {
            try {
                VacuumRobotGeneratorForMLPac.generateVacuum(lst.get(0), lst.get(1) +lst.get(2), lst.get(2));
            } catch(Exception e){
                logger.error(e);
            }
        });
        domainToGenerator.put(Pancakes.class,(lst) -> {
            try {
                PancakesGeneratorForMLPac.generatePancakes(lst.get(0), lst.get(1) + lst.get(2), lst.get(2));
            } catch(Exception e){
                logger.error(e);
            }
        });
    }

    protected static Map<Class,List<Integer>> domainToLevelParams;
    static {
        domainToLevelParams = new HashMap<>();
        // map domain class to <low> , <high> , <delta>
        domainToLevelParams.put(DockyardRobot.class, Arrays.asList(4,6,1));
        domainToLevelParams.put(Pancakes.class, Arrays.asList(15,17,1));
        domainToLevelParams.put(VacuumRobot.class, Arrays.asList(4,6,1));
    }


    public static void main(String[] args){


        Class[] domains = PacConfig.instance.pacPreProcessDomains();
        PacClassifierType[] clsTypes = {PacClassifierType.SMO_REG, PacClassifierType.REGRESSION};

        int numOfFeaturesPerNode = 3;



        for(Class domainClass : domains) {

            List<Integer> domainParams = domainToLevelParams.get(domainClass);

            //generate instances:
            domainToGenerator.get(domainClass).accept(domainParams);

            int trainLevelLow = domainParams.get(0), trainLevelHigh = domainParams.get(1), trainLevelDelta = domainParams.get(2);

            int testLevel = trainLevelHigh + trainLevelDelta;

            OutputResult output = initOutputResultTable(domainClass,testLevel,trainLevelLow ,trainLevelHigh);

            for (PacClassifierType type : clsTypes) {
                train(domainClass, trainLevelLow, trainLevelHigh, trainLevelDelta, numOfFeaturesPerNode, type);

                predict(domainClass, trainLevelLow, trainLevelHigh, testLevel, numOfFeaturesPerNode, type, output);

            }
            if(output != null){
                output.close();
            }

        }

    }

    private static OutputResult initOutputResultTable(Class domainClass, int testLevel, int trainLevelLow, int trainLevelHigh) {

        OutputResult output = null;
        String trainFormat = trainLevelLow + "-" + trainLevelHigh;
        String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
        try {
            output = new OutputResult(outFile, "MLPacStatsPredictionResults_"+ testLevel, true, ".csv");
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

    private static void predict(Class domainClass, int trainLevelLow, int trainLevelHigh, int testLevel, int numOfFeaturesPerNode, PacClassifierType classifierType,OutputResult output) {

            AbstractClassifier classifier = null;
            Instances dataset = null;

            String trainFormat = trainLevelLow + "-" + trainLevelHigh;

            String inFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
            String inputModelPath = inFile+ File.separator + "MLPacStatsPreprocess_"+classifierType+"_"+trainFormat+".model";
            String inputDataPath = inFile + File.separator + "MLPacStatsPreprocess_"+classifierType+"_"+trainFormat+".arff";
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

                // -------------------------------------------------
                // 1. load PAC statistics (to get optimal solutions
                // -------------------------------------------------

//                PACUtils.getPACStatistics(domainClass,DomainExperimentData.RunType.ALL,testLevel);

                // --------------------------------------------------------------
                // 2. for each problem from train: extract features
                // --------------------------------------------------------------

                SearchDomain domain;
                Map<String, String> domainParams = new TreeMap<>();
                Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);

                int fromInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).fromInstance;
                int toInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).toInstance;
                String inputPath = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).pacInputPathFormat, testLevel);

                int size = 39; // calculate automatically

                WAStar optimalSolver = new WAStar();
                optimalSolver.setAdditionalParameter("weight","1.0");

                // go over all training set and extract features:
                for (int i = fromInstance; i <= toInstance; ++i) {
                    logger.info("\rextracting features from " + domainClass.getName() + "\t instance " + i);
                    domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);

                    SearchResult searchResult = optimalSolver.search(domain);
                    double optimalCost = searchResult.getBestSolution().getCost();

                    //--------------------------------------------
                    // extract features from all first nodes:
                    SearchDomain.Operator op;
                    AnytimeSearchNode childNode;

                    SearchDomain.State initialState = domain.initialState();
                    AnytimeSearchNode initialNode = new AnytimeSearchNode(domain,initialState);

                    SearchDomain.State unpackedInitialState = domain.unpack(initialNode.packed);

                    double initialH = initialNode.getH();

                    int indx = 0;
                    Instance ins = new DenseInstance(size);
                    if(dataset == null){
                        return;
                    }

                    ins.setValue(new Attribute("initialH",indx++),initialH);
                    ins.setValue(new Attribute("DomainLevel",indx++),testLevel);

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


                        ins.setValue(new Attribute("childH-"+opIndx,indx++),childH);
                        ins.setValue(new Attribute("childG-"+opIndx,indx++),childG);
                        ins.setValue(new Attribute("childDepth-"+opIndx,indx++),childDepth);

                        // go over gran-children:
                        for(int childOpIndx = 0; childOpIndx < numOfFeaturesPerNode /*domain.getNumOperators(childState)*/; childOpIndx++) {

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

                            ins.setValue(new Attribute("grandchildH-"+opIndx+"-"+childOpIndx,indx++),grandchildH);
                            ins.setValue(new Attribute("grandchildG-"+opIndx+"-"+childOpIndx,indx++),grandchildG);
                            ins.setValue(new Attribute("grandchildDepth-"+opIndx+"-"+childOpIndx,indx++),grandchildDepth);

                        }


                    }
                    ins.setValue(new Attribute("opt-cost",indx++), optimalCost);

                    // Classify
                    ins.setDataset(dataset);
                    double classificationResult = -1;
                    if(classifier == null){
                        return;
                    }
                    try {
                        logger.info(ins.toString());
                        classificationResult = classifier.classifyInstance(ins);
                    } catch (Exception e) {
                        logger.error("ERROR: Failed to classify instance: ",e);
                    }

                    logger.info(classificationResult);

                    output.writeln(i+","+classificationResult+","+optimalCost+","+trainLevelLow+","+trainLevelHigh+","+testLevel+","+ classifier.getClass().getSimpleName() +","+ domainClass.getSimpleName());


                }

            } catch (Exception e) {
                logger.error(e);
            }


    }



    private static void train(Class domainClass, int trainLevelLow, int trainLevelHigh, int trainLevelDelta,int numOfFeaturesPerNode, PacClassifierType classifierType) {
        String outfilePostfix = ".arff";

        OutputResult output = null;
        OutputResult csvOutput = null;
        String trainFormat = trainLevelLow + "-" + trainLevelHigh;
        String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
        try {
            output = new OutputResult(outFile, "MLPacStatsPreprocess_" + classifierType+"_" +trainFormat, true,outfilePostfix);
            csvOutput = new OutputResult(outFile, "MLPacStatsPreprocess_" + classifierType+"_" +trainFormat, true,".csv");
        } catch (IOException e1) {
            logger.error("Failed to create output ML PAC preprocess output file at: " + outFile, e1);
        }

        String tableHeader = MLPacFeatureExtractor.getFeaturesARFFHeaderForPredictedStats(true);
        String csvTableHeader = MLPacFeatureExtractor.getFeaturesCSVHeaderForPredictedStats(true);
        try {
            output.writeln(tableHeader);
            csvOutput.writeln(csvTableHeader);
        } catch (IOException e1) {
            logger.error("Failed to write header to output ML preprocess table: " + tableHeader, e1);
        }

        logger.info("Running anytime for domain " + domainClass.getName());
        try {

            for(int trainLevel = trainLevelLow; trainLevel <= trainLevelHigh; trainLevel += trainLevelDelta) {
                String tableForDomainLevel = extractTableOfFeatures(domainClass, trainLevel, numOfFeaturesPerNode);

                output.writeln(tableForDomainLevel.toString());
                csvOutput.writeln(tableForDomainLevel.toString());
            }
            output.close();
            csvOutput.close();

            // -------------------------------------------------
            // 3. train + save the model to file
            // -------------------------------------------------
            AbstractClassifier cls =
                    MLPacPreprocess.setupAndGetClassifier(output.getFname(), classifierType,false,outFile+ File.separator + "MLPacStatsPreprocess_"+classifierType+"_"+trainFormat+".arff");
            String outputModel = outFile+ File.separator + "MLPacStatsPreprocess_"+classifierType+"_"+trainFormat+".model";
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

    private static String extractTableOfFeatures(Class domainClass, int trainLevel, int numOfFeaturesPerNode){
        logger.info("Solving " + domainClass.getName());

        // -------------------------------------------------
        // 1. load PAC statistics (to get optimal solutions
        // -------------------------------------------------

//        PACUtils.getPACStatistics(domainClass,DomainExperimentData.RunType.ALL,trainLevel);

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

        StringBuilder sb = new StringBuilder();

        // go over all training set and extract features:
        for (int i = fromInstance; i <= toInstance; ++i) {
            logger.info("\rextracting features from " + domainClass.getName() + "\t instance " + i);
            domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);

            SearchResult searchResult = optimalSolver.search(domain);
            double optimalCost = searchResult.getBestSolution().getCost();

            //--------------------------------------------
            // extract features from all first nodes:
            SearchDomain.Operator op;
            AnytimeSearchNode childNode;

            SearchDomain.State initialState = domain.initialState();
            AnytimeSearchNode initialNode = new AnytimeSearchNode(domain,initialState);

            SearchDomain.State unpackedInitialState = domain.unpack(initialNode.packed);


            double initialH = initialNode.getH();



            sb.append(initialH);
            sb.append(",");
            sb.append(trainLevel);
            sb.append(",");

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



                sb.append(childH);
                sb.append(",");
                sb.append(childG);
                sb.append(",");
                sb.append(childDepth);
                sb.append(",");

                // go over gran-children:
                for(int childOpIndx = 0; childOpIndx < numOfFeaturesPerNode /*domain.getNumOperators(childState)*/; childOpIndx++) {

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
                    sb.append(",");
                    sb.append(grandchildG);
                    sb.append(",");
                    sb.append(grandchildDepth);
                    sb.append(",");
                }


            }
            sb.append(optimalCost);
            sb.append("\n");


        }

        return sb.toString();
    }
}
