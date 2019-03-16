package org.cs4j.core.algorithms.pac.conditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.cs4j.core.MLPacFeatureExtractor;
import org.cs4j.core.MLPacFeatureExtractor.PacFeature;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess;
import org.cs4j.core.algorithms.pac.preprocess.ml.MLPacBoundedSolPredictor;
import org.cs4j.core.domains.VacuumRobot;
import org.cs4j.core.experiments.MLPacExperiment;
import org.cs4j.core.mains.DomainExperimentData;

import org.cs4j.core.pac.conf.PacConfig;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.J48;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.*;
import weka.core.converters.CSVLoader;

public class MLPacCondition extends RatioBasedPACCondition {

	private final static Logger logger = Logger.getLogger(MLPacCondition.class);

	protected AbstractClassifier classifier;
	protected ArrayList<Attribute> attributes;
	protected Instances dataset;
	protected static String clsType;
	

	@Override
	public void setup(SearchDomain domain, double epsilon, double delta) {
		super.setup(domain, epsilon, delta);

		// read ML_PAC_Condition_Preprocess.csv and train the model (the output
		// of the training process)
		String inputModelPath = DomainExperimentData.get(domain.getClass(),
				DomainExperimentData.RunType.TRAIN).outputPreprocessPath + "MLPacPreprocess_e"+epsilon+"_"+ this.clsType+".model";
		String inputDataPath = DomainExperimentData.get(domain.getClass(),
				DomainExperimentData.RunType.TRAIN).outputPreprocessPath + "MLPacPreprocess_e"+epsilon+".arff";
		this.setupAttributes();
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

	private void setupAttributes() {
		this.attributes = MLPacFeatureExtractor.getAttributes();
		
	}

	private void getInputInstance(String inputDataPath) {
		logger.debug("getInputInstance | input file: " + inputDataPath);
		CSVLoader loader = new CSVLoader();
		Instances data = null;
		try {
			loader.setSource(new File(inputDataPath));
			data = loader.getDataSet();
		} catch (IOException e) {
			logger.error("ERROR: failed to read input data for classifier: " + inputDataPath,e);
		}


		this.dataset = data;
		this.dataset.setClassIndex(this.dataset.numAttributes() - 1);
	}



	@Override
	public boolean shouldStop(SearchResult incumbentSolution) {

	    //TODO: GAL !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        double fmin = (Double)incumbentSolution.getExtras().get("fmin");
        double incumbent = incumbentSolution.getBestSolution().getCost();
        if (incumbent/fmin <= 1+epsilon)
            return true;

	    //Extract features from an incumbent solution
		Map<PacFeature,Double> features = MLPacFeatureExtractor.extractFeaturesFromSearchResult(incumbentSolution);
		int addedFeatures = PacConfig.instance.useDomainFeatures()? 4 : 0;
		int size = features.size() + addedFeatures;

		// Init a classifier input instance
		Instance ins = new DenseInstance(size + 1);

        // Add features to the input instance
		int indx = 0;

		this.dataset.setClassIndex(this.dataset.numAttributes() - 1);
		ins.setDataset(this.dataset);

		double generated = features.get(PacFeature.GENERATED);
//		Attribute generated = new Attribute("generated");
		ins.setValue(new Attribute("generated",indx++),generated);
		double expanded = features.get(PacFeature.EXPANDED);
		ins.setValue(new Attribute("expanded",indx++),expanded);
		double reopened = features.get(PacFeature.ROPENED);
		ins.setValue(new Attribute("reopened",indx++),reopened);
		double U = features.get(PacFeature.COST);
		ins.setValue(new Attribute("cost",indx++),U);
		double g1 = features.get(PacFeature.G_0);
		ins.setValue(new Attribute("g1",indx++),g1);
		double h1 = features.get(PacFeature.H_0);
		ins.setValue(new Attribute("h1",indx++),h1);
		double g2 = features.get(PacFeature.G_2);
		ins.setValue(new Attribute("g2",indx++),g2);
		double h2 = features.get(PacFeature.H_2);
		ins.setValue(new Attribute("h2",indx++),h2);
		double g3 = features.get(PacFeature.G_2);
		ins.setValue(new Attribute("g3",indx++),g3);
		double h3 = features.get(PacFeature.H_2);
		ins.setValue(new Attribute("h3",indx++),h3);
		double w = 1.0 + (Double) incumbentSolution.getExtras().get("epsilon");
		ins.setValue(new Attribute("w",indx++),w);


		if(PacConfig.instance.useDomainFeatures()) {
			VacuumRobot.VacuumRobotState start = (VacuumRobot.VacuumRobotState) incumbentSolution.getBestSolution().getStates().get(0);
			VacuumRobot.VacuumRobotState  goal = (VacuumRobot.VacuumRobotState) incumbentSolution.getBestSolution().getStates().get(incumbentSolution.getBestSolution().getStates().size() -1);

			Map<PacFeature, Double> startMap = VacuumRobot.dumpStateArray(start);
			double remainingDirtyLocationCount_start = startMap.get(PacFeature.remainingDirtyLocationsCount);
			double dirtyVector_start = startMap.get(PacFeature.dirtyVector);

			Map<PacFeature, Double> goaltMap = VacuumRobot.dumpStateArray(goal);
			double remainingDirtyLocationCount_goal = goaltMap.get(PacFeature.remainingDirtyLocationsCount);
			double dirtyVector_goal = goaltMap.get(PacFeature.dirtyVector);

			ins.setValue(new Attribute("remainingDirtyLocationCount_start", indx++), dirtyVector_start);
			ins.setValue(new Attribute("dirtyVector_start", indx++), dirtyVector_start);

			ins.setValue(new Attribute("remainingDirtyLocationCount_goal", indx++), remainingDirtyLocationCount_goal);
			ins.setValue(new Attribute("dirtyVector_goal", indx++), dirtyVector_goal);
		}
		FastVector fvNominalVal = new FastVector(2);
		fvNominalVal.addElement("true");
		fvNominalVal.addElement("false");
		Attribute classAtt = new Attribute("is-W-opt", fvNominalVal,indx++);
		ins.setValue( classAtt,"false");

		logger.info("instance to classify: "+ins.toString());


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
