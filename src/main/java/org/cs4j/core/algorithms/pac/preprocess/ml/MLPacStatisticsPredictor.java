package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.apache.log4j.Logger;
import org.cs4j.core.MLPacFeatureExtractor;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess;
import org.cs4j.core.algorithms.pac.preprocess.PacClassifierType;
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
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Gal Dreiman on 22/05/2017.
 */
public class MLPacStatisticsPredictor {
    private final static Logger logger = Logger.getLogger(MLPacStatisticsPredictor.class);
    private static OutputResult output;



    public static void main(String[] args){


        Class[] domains = PacConfig.instance.pacPreProcessDomains();//{  VacuumRobot.class};//, VacuumRobot.class,  Pancakes.class};
        PacClassifierType[] clsTypes = {PacClassifierType.SMO_REG, PacClassifierType.REGRESSION};

        int numOfFeaturesPerNode = 3;

        for(int trainLevel = 10; trainLevel < 45; trainLevel += 5) {
            for (PacClassifierType type : clsTypes) {
                train(domains, trainLevel, numOfFeaturesPerNode, type);

                predict(domains, trainLevel, trainLevel + 5, numOfFeaturesPerNode, type);
            }
        }

    }

    private static void predict(Class[] domains, int trainLevel, int testLevel, int numOfFeaturesPerNode, PacClassifierType classifierType) {
        for (Class domainClass : domains) {
            AbstractClassifier classifier = null;
            Instances dataset = null;



            String inFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainLevel);
            String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, testLevel);
            String inputModelPath = inFile+ File.separator + "MLPacStatsPreprocess_"+classifierType+".model";
            String inputDataPath = inFile + File.separator + "MLPacStatsPreprocess_"+classifierType+".arff";
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



            try {
                output = new OutputResult(outFile, "MLPacStatsPredictionResults", true, ".csv");
            } catch (IOException e1) {
                logger.error("Failed to create output ML PAC preprocess output file at: " + outFile, e1);
            }

            String tableHeader = "instance_id, h*_prediction, h*_actual, trainLevel, testLevel, classifier, domain";
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

                PACUtils.getPACStatistics(domainClass,DomainExperimentData.RunType.ALL,testLevel);

                // --------------------------------------------------------------
                // 2. for each problem from train: extract features
                // --------------------------------------------------------------

                SearchDomain domain;
                Map<String, String> domainParams = new TreeMap<>();
                Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);

                int fromInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).fromInstance;
                int toInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).toInstance;
                String inputPath = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).pacInputPathFormat, testLevel);

                int size = 38; // calculate automatically

                // go over all training set and extract features:
                for (int i = fromInstance; i <= toInstance; ++i) {
                    logger.info("\rextracting features from " + domainClass.getName() + "\t instance " + i);
                    domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);

                    double optimalCost = PACUtils.getOptimalSolution(domainClass, i);

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

                    output.writeln(i+","+classificationResult+","+optimalCost+","+trainLevel+","+testLevel+","+ classifier.getClass().getSimpleName() +","+ domainClass.getSimpleName());


                }
                output.close();

            } catch (Exception e) {
                logger.error(e);
            } finally {
                if (output != null) {
                    output.close();
                }
            }

        }
    }

    private static void train(Class[] domains, int trainLevel,int numOfFeaturesPerNode, PacClassifierType classifierType) {
        String outfilePostfix = ".arff";

        for (Class domainClass : domains) {


                String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainLevel);
                try {
                    output = new OutputResult(outFile, "MLPacStatsPreprocess_" + classifierType, true,outfilePostfix);
                } catch (IOException e1) {
                    logger.error("Failed to create output ML PAC preprocess output file at: " + outFile, e1);
                }

                String tableHeader = MLPacFeatureExtractor.getFeaturesARFFHeaderForPredictedStats(true);
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

                        //--------------------------------------------
                        // extract features from all first nodes:
                        SearchDomain.Operator op;
                        AnytimeSearchNode childNode;

                        SearchDomain.State initialState = domain.initialState();
                        AnytimeSearchNode initialNode = new AnytimeSearchNode(domain,initialState);

                        SearchDomain.State unpackedInitialState = domain.unpack(initialNode.packed);
                        StringBuilder sb = new StringBuilder();

                        double initialH = initialNode.getH();



                        sb.append(initialH);
                        sb.append(" ");

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
                            sb.append(" ");
                            sb.append(childG);
                            sb.append(" ");
                            sb.append(childDepth);
                            sb.append(" ");

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
                                sb.append(" ");
                                sb.append(grandchildG);
                                sb.append(" ");
                                sb.append(grandchildDepth);
                                sb.append(" ");
                            }


                        }
                        sb.append(optimalCost);
                        output.writeln(sb.toString().replace(" ", ","));


                    }
                    output.close();

                    // -------------------------------------------------
                    // 3. train + save the model to file
                    // -------------------------------------------------
                    AbstractClassifier cls =
                            MLPacPreprocess.setupAndGetClassifier(output.getFname(), classifierType,false,outFile+ File.separator + "MLPacStatsPreprocess_"+classifierType+".arff");
                    String outputModel = outFile+ File.separator + "MLPacStatsPreprocess_"+classifierType+".model";
                    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputModel));
                    oos.writeObject(cls);
                    oos.flush();




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
