package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.apache.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.algorithms.pac.AnytimePTS4PAC;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.conditions.MLPacConditionForBoundSolPredNN;
import org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess;
import org.cs4j.core.algorithms.pac.preprocess.PacClassifierType;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.pac.conf.MLPacPreprocessExperimentValues;
import org.cs4j.core.pac.conf.PacConfig;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Gal Dreiman on 05/08/2017.
 */
public class MLPacBoundedSolPredictor {
    private final static Logger logger = Logger.getLogger(MLPacBoundedSolPredictor.class);

    private static final String modelFileFormat ="MLPacBoundedSolPreprocess_e%s_c_%s_tl_%s.model"; // <epsilon>,<classifierType>,<trainFormat>
    private static final String dataFileFormat ="MLPacBoundedSolPreprocess_e%s_c_%s_tl_%s.arff"; // <epsilon>,<classifierType>,<trainFormat>

    public static void main(String[] args) {


        MLPacPreprocessExperimentValues[] experimentValuesList = PacConfig.instance.predictionDomainsAndExpValues();

        for(MLPacPreprocessExperimentValues experimentValues : experimentValuesList) {

            PacClassifierType[] clsTypes = {PacClassifierType.NN};
            double[] epsilons = PacConfig.instance.inputPredictionEpsilons();
            Class domainClass = experimentValues.getDomainClass();


            //generate instances:
            MLPacHStarPredictor.domainToGenerator.get(domainClass).accept(experimentValues);

            // feature extraction + training:

            int trainLevelLow = experimentValues.getTrainLevelLow(), trainLevelHigh = experimentValues.getTrainLevelHigh(), trainLevelDelta = experimentValues.getTrainLevelDelta();
            for (PacClassifierType type : clsTypes) {

                //-----------------------------------------------------------------
                String outfilePostfix = ".arff";

                OutputResult output = null;
                OutputResult csvOutput = null;
                String trainFormat = trainLevelLow + "-" + trainLevelHigh;
                String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
                try {
                    output = new OutputResult(outFile, "MLPacBoundedSolPreprocess_c_" + type+"_tl_" +trainFormat, true,outfilePostfix);
                    csvOutput = new OutputResult(outFile, "MLPacBoundedSolPreprocess_c_"+ type+"_tl_" +trainFormat, true,".csv");
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
                //-----------------------------------------------------------------
                for (double epsilon : epsilons) {
                    train(domainClass, epsilon, trainLevelLow, trainLevelHigh, trainLevelDelta, type,output,csvOutput,outFile);
                }
                try {
                trainClassifier(type, output, outFile, trainFormat);
                }catch (Exception e){
                    logger.error("Failed to trainClassifier:   type: "+type+ "  trainFormat: " + trainFormat, e);
                }

                if (output != null) {
                    output.close();
                }
                if(csvOutput != null){
                    csvOutput.close();
                }
            }


            // prediction:
            int testLevel = trainLevelHigh + trainLevelDelta;
            OutputResult output = initOutputResultTable(domainClass, testLevel, trainLevelLow, trainLevelHigh);


                for (double epsilon : epsilons) {
                    for (PacClassifierType type : clsTypes) {
                        OutputResult outputRawPredictions = initOutputRawResultTable(domainClass, trainLevelLow, trainLevelHigh, testLevel, type);
                        predict(domainClass, epsilon, trainLevelLow, trainLevelHigh, testLevel, type, output,outputRawPredictions);
                        outputRawPredictions.close();
                        try {
                            evaluatePrediction(domainClass, trainLevelLow, trainLevelHigh, testLevel, type);
                        }catch (Exception e){
                            logger.error("Failed to evaluate classifier for: epsilon"+ epsilon +", classifier type "+type +",  domain level " + testLevel);
                        }
                    }
                }

                if (output != null) {
                    output.close();
                }
        }
        logger.info("Done!");

    }

    private static void evaluatePrediction(Class domainClass, int trainLevelLow, int trainLevelHigh, int testLevel, PacClassifierType type) throws Exception{
//        String trainFormat = trainLevelLow + "-" + trainLevelHigh;
//        String inputTestDataDir = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
//        String inputTestDataFile = "MLPacBoundedSolPreprocessRawPredictions_c_" + type+"_train_"+trainFormat+"_test_" +testLevel +".arff";
//        String inputTrainDataFile =
//
//        Instances testData = ConverterUtils.DataSource.read(inputTestDataDir +File.separator+ inputTestDataFile);
//        Classifier classifier;
//        if(type.equals(PacClassifierType.NN)) {
//            classifier = new MultilayerPerceptron();
//            ((MultilayerPerceptron) classifier).setLearningRate(0.1);
//            ((MultilayerPerceptron) classifier).setMomentum(0.2);
//            ((MultilayerPerceptron) classifier).setTrainingTime(2000);
//            ((MultilayerPerceptron) classifier).setHiddenLayers("3");
//        }
//        else {
//            return;
//        }
//
//        Evaluation eval = new Evaluation(testData);
    }

    private static void predict(Class domainClass,double epsilon, int trainLevelLow, int trainLevelHigh, int testLevel, PacClassifierType classifierType,OutputResult output,OutputResult outputRawPredictions) {

        AbstractClassifier classifier = null;
        Instances dataset = null;

        double[] deltas = PacConfig.instance.inputPredictionDeltas();
        String trainFormat = trainLevelLow + "-" + trainLevelHigh;

        String inFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
        String inputModelPath = inFile+ File.separator + String.format(modelFileFormat,epsilon,classifierType,trainFormat);
        String inputDataPath = inFile + File.separator + String.format(dataFileFormat,epsilon,classifierType,trainFormat);
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
            String inputPath = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).pacInputPathFormat, ((int)testLevel));

            int size = 13; // calculate automatically

            WAStar optimalSolver = new WAStar();
            optimalSolver.setAdditionalParameter("weight","1.0");

            for(double delta : deltas) {
                PACSearchFramework psf = new PACSearchFramework();
                psf.setAnytimeSearchClass(AnytimePTS4PAC.class);
                psf.setPACConditionClass(MLPacConditionForBoundSolPredNN.class);
                psf.setAdditionalParameter("anytimeSearch", AnytimePTS4PAC.class.getName());
                psf.setTrainLevel(trainLevelLow + "-" + trainLevelHigh);
                psf.setDomainLevel(testLevel);
                psf.setOutputResult(outputRawPredictions);
                psf.setAdditionalParameter("epsilon", epsilon + "");
                psf.setAdditionalParameter("delta", delta + "");

                // go over all training set and extract features:
                for (int i = fromInstance; i <= toInstance; ++i) {
                    logger.info("\rextracting features from " + domainClass.getName() + "\t instance " + i);
                    domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);

                    SearchResult searchResult = null;
                    try {
                        searchResult = optimalSolver.search(domain);
                    } catch(Exception e){
                        logger.error("Failed to solve "+ domainClass.getSimpleName()+" instance #"+ i +" optimally. got: " +e);
                        continue;
                    }
                    if(!searchResult.hasSolution()){
                        continue;
                    }
                    double optimalCost = searchResult.getBestSolution().getCost();

                    psf.setCurrentOptimalCost(optimalCost);
                    SearchResult result = psf.search(domain);
                    SearchResult.Solution bestSolution = result.getBestSolution();

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


    private static void train(Class domainClass,double epsilon, int trainLevelLow, int trainLevelHigh, int trainLevelDelta, PacClassifierType classifierType,OutputResult output,OutputResult csvOutput,String outFile) {

        String trainFormat = trainLevelLow + "-" + trainLevelHigh;
        logger.info("Running anytime for domain " + domainClass.getName());
        try {

            for(int trainLevel = trainLevelLow; trainLevel <= trainLevelHigh; trainLevel += trainLevelDelta) {
                String tableForDomainLevel = extractTableOfFeatures(domainClass, trainLevel, epsilon);

                output.writeln(tableForDomainLevel.toString());
                csvOutput.writeln(tableForDomainLevel.toString());
            }



        } catch (Exception e) {
            logger.error(e);
        }

    }

    private static void trainClassifier(PacClassifierType classifierType, OutputResult output, String outFile, String trainFormat) throws IOException {
        // -------------------------------------------------
        // 3. train + save the model to file
        // -------------------------------------------------
        AbstractClassifier cls =
                MLPacPreprocess.setupAndGetClassifier(output.getFname(), classifierType,false,outFile+ File.separator + "MLPacBoundedSolPreprocess_"+classifierType+"_"+trainFormat+".arff");
        String outputModel = outFile+ File.separator + "MLPacBoundedSolPreprocess_c_"+classifierType+"_tl_"+trainFormat+".model";
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputModel));
        oos.writeObject(cls);
        oos.flush();
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
            if(!optSearchResult.hasSolution()){
                continue;
            }
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
                double h1ToLevel = h1/trainLevel;

                double g2 = features.get(MLPacFeatureExtractor.PacFeature.G_2);
                double h2 = features.get(MLPacFeatureExtractor.PacFeature.H_2);
                double h2ToLevel = h2/trainLevel;

                double g3 = features.get(MLPacFeatureExtractor.PacFeature.G_2);
                double h3 = features.get(MLPacFeatureExtractor.PacFeature.H_2);
                double h3ToLevel = h3/trainLevel;

                double w = 1.0 + (Double) searchResult.getExtras().get("epsilon");

                boolean isWOptimal =  optimalCost * (1 + epsilon) >= cost;

                String[] lineParts = {generated+"",expanded+"",reopened+"", trainLevel+"",U+"",g1+"",h1+"",h1ToLevel+"",g2+"",h2+"",h2ToLevel+"",g3+"",h3+"",h3ToLevel+"",w+"",isWOptimal+"\n"};
                String line = String.join(",", lineParts);
                sb.append(line);

                searchResult = solver.continueSearch();

            }
        }

        return sb.toString();
    }

    private static OutputResult initOutputResultTable(Class domainClass, int testLevel, int trainLevelLow, int trainLevelHigh) {

        OutputResult output = null;
        String trainFormat = trainLevelLow + "-" + trainLevelHigh;
        String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
        try {
            output = new OutputResult(outFile, "MLPacBoundedSolPredictionResults_tl_"+ testLevel, true, ".csv");
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

    private static OutputResult initOutputRawResultTable(Class domainClass, int trainLevelLow, int trainLevelHigh, int domainLevel,PacClassifierType classifierType) {

        OutputResult output = null;
        String trainFormat = trainLevelLow + "-" + trainLevelHigh;
        String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
        try {
            output = new OutputResult(outFile, "MLPacBoundedSolPreprocessRawPredictions_c_" + classifierType+"_train_"+trainFormat+"_test_" +domainLevel, true,".arff");
        } catch (IOException e1) {
            logger.error("Failed to create output ML PAC preprocess output file at: " + outFile, e1);
        }

        String tableHeader = MLPacFeatureExtractor.getFeaturesARFFHeaderBoundSolPred();
        try {
            output.writeln(tableHeader);
        } catch (IOException e1) {
            logger.error("Failed to write header to output ML preprocess table: " + tableHeader, e1);
        }
        return output;
    }
}
