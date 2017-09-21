package org.cs4j.core.algorithms.pac.conditions;

import org.apache.log4j.Logger;
import org.cs4j.core.MLPacFeatureExtractor;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess;
import org.cs4j.core.algorithms.pac.preprocess.ml.MLPacBoundedSolPredictor;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.pac.conf.PacConfig;
import weka.classifiers.AbstractClassifier;
import weka.core.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Gal Dreiman on 05/08/2017.
 */
public class MLPacConditionForBoundSolPred extends MLPacCondition {
    private final static Logger logger = Logger.getLogger(MLPacConditionForBoundSolPred.class);


    protected static String trainFormat;
    protected static double domainLevel;
    protected static OutputResult output;
    protected static double currentOptimalCost;




    @Override
    public void setup(SearchDomain domain, double epsilon, double delta) {
        super.setup(domain, epsilon, delta);

        // read ML_PAC_Condition_Preprocess.csv and train the model (the output
        // of the training process)
        String inputModelPath = String.format(DomainExperimentData.get(domain.getClass(),
                DomainExperimentData.RunType.ALL).outputPreprocessPathFormat,trainFormat)+ File.separator + "MLPacBoundedSolPreprocess_e"+epsilon+"_c_" + clsType+"_tl_" + trainFormat+".model";
        String inputDataPath = String.format(DomainExperimentData.get(domain.getClass(),
                DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, trainFormat)+ File.separator + "MLPacBoundedSolPreprocess_e"+epsilon+"_c_" + clsType+"_tl_" +trainFormat+".arff";

        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputModelPath));
            this.classifier = (AbstractClassifier) ois.readObject();
            ois.close();

            this.dataset = MLPacPreprocess.getInputInstance(inputDataPath);
            this.dataset.setClassIndex(this.dataset.numAttributes() - 1);
        } catch (Exception e) {
            logger.error("Failed to load model for input file [" +
                    inputDataPath + "]", e);
        }
    }

    public void setTrainLevel(String trainFormat){
        this.trainFormat = trainFormat;
    }
    public void setDomainLevel(double domainLevel){
        this.domainLevel = domainLevel;
    }
    public void setOutputResult(OutputResult output){ this.output = output; }
    public void setCurrentOptimalCost(double currentOptimalCost){
        this.currentOptimalCost = currentOptimalCost;
    }

    @Override
    public boolean shouldStop(SearchResult incumbentSolution) {

        double fmin = (Double)incumbentSolution.getExtras().get("fmin");
        double incumbent = incumbentSolution.getBestSolution().getCost();
        if (incumbent/fmin <= 1+epsilon)
            return true;

        //Extract features from an incumbent solution
        Map<MLPacFeatureExtractor.PacFeature,Double> features = MLPacFeatureExtractor.extractFeaturesFromSearchResult(incumbentSolution);
        int size = features.size() + 4;

        // Init a classifier input instance
        Instance ins = new DenseInstance(size + 1);
        StringBuilder sb = new StringBuilder();

        // Add features to the input instance
        int indx = 0;

        this.dataset.setClassIndex(this.dataset.numAttributes() - 1);
        ins.setDataset(this.dataset);


        double generated = features.get(MLPacFeatureExtractor.PacFeature.GENERATED);
        ins.setValue(new Attribute("generated",indx++),generated);

        double expanded = features.get(MLPacFeatureExtractor.PacFeature.EXPANDED);
        ins.setValue(new Attribute("expanded",indx++),expanded);

        double reopened = features.get(MLPacFeatureExtractor.PacFeature.ROPENED);
        ins.setValue(new Attribute("reopened",indx++),reopened);

        ins.setValue(new Attribute("domainLevel",indx++),domainLevel);

        double U = features.get(MLPacFeatureExtractor.PacFeature.COST);
        ins.setValue(new Attribute("cost",indx++),U);

        double g1 = features.get(MLPacFeatureExtractor.PacFeature.G_0);
        ins.setValue(new Attribute("g1",indx++),g1);

        double h1 = features.get(MLPacFeatureExtractor.PacFeature.H_0);
        ins.setValue(new Attribute("h1",indx++),h1);

        double h1ToLevel = h1/this.domainLevel;
        ins.setValue(new Attribute("h1ToLevel",indx++),h1ToLevel);
        sb.append(generated); sb.append(",");

        double g2 = features.get(MLPacFeatureExtractor.PacFeature.G_2);
        ins.setValue(new Attribute("g2",indx++),g2);

        double h2 = features.get(MLPacFeatureExtractor.PacFeature.H_2);
        ins.setValue(new Attribute("h2",indx++),h2);

        double h2ToLevel = h2/this.domainLevel;
        ins.setValue(new Attribute("h2ToLevel",indx++),h2ToLevel);

        double g3 = features.get(MLPacFeatureExtractor.PacFeature.G_2);
        ins.setValue(new Attribute("g3",indx++),g3);

        double h3 = features.get(MLPacFeatureExtractor.PacFeature.H_2);
        ins.setValue(new Attribute("h3",indx++),h3);

        double h3ToLevel = h3/this.domainLevel;
        ins.setValue(new Attribute("h3ToLevel",indx++),h3ToLevel);

        double w = 1.0 + (Double) incumbentSolution.getExtras().get("epsilon");
        ins.setValue(new Attribute("w",indx++),w);

        FastVector fvNominalVal = new FastVector(2);
        fvNominalVal.addElement("true");
        fvNominalVal.addElement("false");
        Attribute classAtt = new Attribute("is-W-opt", fvNominalVal,indx++);
        boolean isEpsilon = this.currentOptimalCost * (1 + epsilon) >= U;
        ins.setValue( classAtt,isEpsilon+"");

        String instanceFeatures = ins.toString();
        logger.info("instance to classify: "+instanceFeatures);

        try {
            this.output.writeln(instanceFeatures);
        } catch (IOException e) {
            logger.error("Failed to write instance features: " + instanceFeatures, e);
        }

        if(PacConfig.instance.outputTestRawFeatures()){
            try {
                String outputDir = String.format(DomainExperimentData.get(this.domain.getClass(), DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, MLPacBoundedSolPredictor.trainFormatForPRedictionOutput);
                String outFile = outputDir +File.separator + "MLPacPredictionForHardProbs_e"+epsilon +"d_"+delta+".arff";
                String line = ins.toString() +"\n";
                Files.write(Paths.get(outFile), line.toString().getBytes(), StandardOpenOption.APPEND);
            }catch (IOException e) {
                //exception handling left as an exercise for the reader
            }
        }

        double[] distributeResult = {};
        double classificationResult = -1;

        // Classify
        try {
            classificationResult = this.classifier.classifyInstance(ins);
            distributeResult = this.classifier.distributionForInstance(ins); // TODO: GAL WILL CHECK IF WE NEED THE ZERO OR THE ONE CLASS
        } catch (Exception e) {
            logger.error("ERROR: Failed to classify instance: ",e);
        }

        if(distributeResult.length <= 1){
            // in case of only one or less labels the classifier probably did not trained properly on more then one label
            return false;
        }

        logger.debug("Classifier type: " +this.classifier.getClass().getSimpleName());
        logger.debug("distribute  result for instance: [" + ins.toString() +"] is ["+ distributeResult[0] +"] ["+ distributeResult[1] +"]");
        logger.debug("classification  result for instance: [" + ins.toString() +"] is ["+ classificationResult +"]");
        boolean pacConditionResult = distributeResult[0] >= (1-this.delta);
        return pacConditionResult;
    }

}
