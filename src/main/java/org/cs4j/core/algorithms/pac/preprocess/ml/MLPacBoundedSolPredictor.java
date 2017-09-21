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
import org.cs4j.core.pac.evaluation.PacAttribute;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Created by Gal Dreiman on 05/08/2017.
 */
public class MLPacBoundedSolPredictor {
    private final static Logger logger = Logger.getLogger(MLPacBoundedSolPredictor.class);

    public static final String modelFileFormat ="MLPacBoundedSolPreprocess_e_%s_c_%s_tl_%s.model"; // <epsilon>,<classifierType>,<trainFormat>
    public static final String dataFileFormat = "MLPacBoundedSolPreprocess_e_%s_c_%s_tl_%s.arff"; // <epsilon>,<classifierType>,<trainFormat>

    public static String trainFormatForPRedictionOutput = "";

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
            int testLevel = experimentValues.getTestLevel();
            for (PacClassifierType type : clsTypes) {
                for (double epsilon : epsilons) {
                    train(domainClass, epsilon, trainLevelLow, trainLevelHigh, trainLevelDelta, type);
                }
            }


            // prediction:
            OutputResult output = initOutputResultTable(domainClass, testLevel, trainLevelLow, trainLevelHigh);
            OutputResult evaluationOutput = getEvaluationOutputResult(domainClass,trainLevelLow + "-" + trainLevelHigh);

            trainFormatForPRedictionOutput = trainLevelLow + "-" + trainLevelHigh;

            for (double epsilon : epsilons) {
                for (PacClassifierType type : clsTypes) {
                    OutputResult outputRawPredictions = initOutputRawResultTable(domainClass,epsilon, trainLevelLow, trainLevelHigh, testLevel, type);
                    predict(domainClass, epsilon, trainLevelLow, trainLevelHigh, testLevel, type, output,outputRawPredictions);
                    outputRawPredictions.close();

                    if(PacConfig.instance.PredictionApplyEvaluation()) {
                        try {
                            evaluatePrediction(domainClass,epsilon, trainLevelLow, trainLevelHigh,trainLevelDelta, testLevel, type,evaluationOutput);
                            evaluationOutput.writeln("");
                        }catch (Exception e){
                            logger.error("Failed to evaluate classifier for: epsilon"+ epsilon +", classifier type "+type +",  domain level " + testLevel, e);
                        }
                    }

                }
            }
            if (output != null) {
                output.close();
            }
            if (evaluationOutput != null) {
                evaluationOutput.close();
            }

        }
        logger.info("Done!");

    }

    public static void evaluatePrediction(Class domainClass,double epsilon, int trainLevelLow, int trainLevelHigh,int trainLevelDelta, int testLevel, PacClassifierType type,OutputResult evaluationOutput) throws Exception{
        String trainFormat = trainLevelLow + "-" + trainLevelHigh;
        String inputTestDataDir = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);

        String dateFileName = String.format(MLPacBoundedSolPredictor.dataFileFormat,epsilon,type, trainFormat);

        String inputTestDataFile = "MLPacBoundedSolPreprocessRawPredictions_e_"+epsilon+"_c_" + type+"_train_"+trainFormat+"_test_" +testLevel +".arff";
        String inputTrainDataFile = dateFileName;

        int classIndex = 15;

        Instances trainData = ConverterUtils.DataSource.read(inputTestDataDir +File.separator+ inputTrainDataFile);
        trainData.setClassIndex(classIndex);
        Instances testData = ConverterUtils.DataSource.read(inputTestDataDir +File.separator+ inputTestDataFile);
        testData.setClassIndex(classIndex);
        Classifier classifier;
        if(type.equals(PacClassifierType.NN)) {
            classifier = new MultilayerPerceptron();
            ((MultilayerPerceptron) classifier).setLearningRate(0.1);
            ((MultilayerPerceptron) classifier).setMomentum(0.2);
            ((MultilayerPerceptron) classifier).setTrainingTime(2000);
            ((MultilayerPerceptron) classifier).setHiddenLayers("3");
        }
        else {
            return;
        }
        classifier.buildClassifier(trainData);
        Evaluation eval = new Evaluation(trainData);
        eval.evaluateModel(classifier, testData);
        System.out.println(eval.toSummaryString("\nResults\n======\n", false));

        ThresholdCurve tc = new ThresholdCurve();
        Instances curve = tc.getCurve(eval.predictions(), 0);

        for(int i = 0; i < curve.numInstances(); ++i){

            ArrayList<String> row = new ArrayList<>();

            for(int insIndx = 0; insIndx < curve.get(i).numAttributes(); ++insIndx){
                row.add(curve.get(i).value(insIndx) +"");
            }
            row.add(domainClass.getSimpleName());
            row.add(""+epsilon);
            row.add(""+ThresholdCurve.getROCArea(curve));
            row.add(trainFormat);
            row.add(trainLevelDelta+"");


            evaluationOutput.writeln(row.stream().collect(Collectors.joining(",")));

        }
    }

    public static void saveInstancesToFile( ArrayList<String> outputTable, String outputTestDataDir) throws IOException {
        String outputCsvFile = "Out_MLPacPreprocess_Evaluation.csv";

        String table = outputTable.stream().collect(Collectors.joining("\n"));

        try(  PrintWriter out = new PrintWriter( outputCsvFile )  ){
            out.println( table );
        }
    }

    public static OutputResult getEvaluationOutputResult(Class domainClass, String trainFormat){

        String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
        OutputResult output = null;
        try {
            output = new OutputResult(outFile, "Out_MLPacPreprocess_Evaluation", true,".csv");
        } catch (IOException e1) {
            logger.error("Failed to create output ML PAC preprocess output file at: " + outFile, e1);
        }

        ArrayList<Attribute> fv = new ArrayList<>();
        fv.add(new PacAttribute(ThresholdCurve.TRUE_POS_NAME,0,false));
        fv.add(new PacAttribute(ThresholdCurve.FALSE_NEG_NAME,1,false));
        fv.add(new PacAttribute(ThresholdCurve.FALSE_POS_NAME,2,false));
        fv.add(new PacAttribute(ThresholdCurve.TRUE_NEG_NAME,3,false));
        fv.add(new PacAttribute(ThresholdCurve.FP_RATE_NAME,4,false));
        fv.add(new PacAttribute(ThresholdCurve.TP_RATE_NAME,5,false));
        fv.add(new PacAttribute(ThresholdCurve.PRECISION_NAME,6,false));
        fv.add(new PacAttribute(ThresholdCurve.RECALL_NAME,7,false));
        fv.add(new PacAttribute(ThresholdCurve.FALLOUT_NAME,8,false));
        fv.add(new PacAttribute(ThresholdCurve.FMEASURE_NAME,9,false));
        fv.add(new PacAttribute(ThresholdCurve.SAMPLE_SIZE_NAME,10,false));
        fv.add(new PacAttribute(ThresholdCurve.LIFT_NAME,11,false));
        fv.add(new PacAttribute(ThresholdCurve.THRESHOLD_NAME,12,false));


        String headerTable = fv.stream().map(att -> att.name()).collect(Collectors.joining(","));
        headerTable += ",Delta,Epsilon,AUC,trainFormat,trainLevelDelta";
        try {
            output.writeln(headerTable);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return output;
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

                if(PacConfig.instance.outputTestRawFeatures()){
                    String tableHeader = MLPacFeatureExtractor.getFeaturesARFFHeaderBoundSolPred();
                    try {
                        String outputDir = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, MLPacBoundedSolPredictor.trainFormatForPRedictionOutput);
                        String outFile = outputDir +File.separator + "MLPacPredictionForHardProbs_e"+epsilon +"d_"+delta+".arff";
                        File f = new File(outFile);
                        if(f.exists()){
                            f.delete();
                        }
                        Files.write(Paths.get(outFile), tableHeader.getBytes(), StandardOpenOption.CREATE_NEW);
                    }catch (IOException e) {
                        //exception handling left as an exercise for the reader
                    }
                }

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
                    logger.info("\rextracting features from " + domainClass.getName() + "\t instance " + i +"  |  epsilon: "+ epsilon + "  |   delta: " + delta);
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


    private static void train(Class domainClass,double epsilon, int trainLevelLow, int trainLevelHigh, int trainLevelDelta, PacClassifierType classifierType) {

        String trainFormat = trainLevelLow + "-" + trainLevelHigh;
        OutputResult output = null;
        OutputResult csvOutput = null;
        String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
        String dateFileName = String.format(MLPacBoundedSolPredictor.dataFileFormat,epsilon,classifierType, trainFormat);
        try {
            output = new OutputResult(outFile, dateFileName, true,"");
            csvOutput = new OutputResult(outFile, "MLPacBoundedSolPreprocess_e_"+epsilon+"_c_"+ classifierType+"_tl_" +trainFormat, true,".csv");
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


        } catch (Exception e) {
            logger.error(e);
        }

        if (output != null) {
            output.close();
        }
        if(csvOutput != null){
            csvOutput.close();
        }

        // -------------------------------------------------
        //  train + save the model to file
        // -------------------------------------------------
        try {
            String modelFileName = String.format(MLPacBoundedSolPredictor.modelFileFormat,epsilon,classifierType, trainFormat);
            String datasetFilePath = outFile+ File.separator + dateFileName;
            AbstractClassifier cls =
                    MLPacPreprocess.setupAndGetClassifier(output.getFname(), classifierType,false,datasetFilePath);
            String outputModel = outFile+ File.separator + modelFileName;
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputModel));
            oos.writeObject(cls);
            oos.flush();
        } catch (Exception e) {
            logger.error(e);
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
            if(!optSearchResult.hasSolution()){
                continue;
            }
            double optimalCost = optSearchResult.getBestSolution().getCost();

            // get anytime solution:
            SearchResult searchResult = solver.search(domain);
            while (searchResult.hasSolution()){

                double fmin = (Double)searchResult.getExtras().get("fmin");
                double incumbent = searchResult.getBestSolution().getCost();
                if (incumbent/fmin <= 1+epsilon)
                    break;


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

    private static OutputResult initOutputRawResultTable(Class domainClass,double epsilon, int trainLevelLow, int trainLevelHigh, int domainLevel,PacClassifierType classifierType) {

        OutputResult output = null;
        String trainFormat = trainLevelLow + "-" + trainLevelHigh;
        String outFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat);
        try {
            output = new OutputResult(outFile, "MLPacBoundedSolPreprocessRawPredictions_e_"+epsilon+"_c_" + classifierType+"_train_"+trainFormat+"_test_" +domainLevel, true,".arff");
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
