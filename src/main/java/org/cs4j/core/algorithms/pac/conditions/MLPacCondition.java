package org.cs4j.core.algorithms.pac.conditions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.cs4j.core.MLPacFeatureExtractor;
import org.cs4j.core.MLPacFeatureExtractor.PacFeature;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.mains.DomainExperimentData;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.J48;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

public class MLPacCondition extends RatioBasedPACCondition {

	private final static Logger logger = Logger.getLogger(MLPacCondition.class);

	private AbstractClassifier classifier;
	private ArrayList<Attribute> attributes;
	private Instances dataset;
	

	@Override
	public void setup(SearchDomain domain, double epsilon, double delta) {
		super.setup(domain, epsilon, delta);

		// read ML_PAC_Condition_Preprocess.csv and train the model (the output
		// of the training process)
		String inputDataPath = DomainExperimentData.get(domain.getClass(),
				DomainExperimentData.RunType.TRAIN).outputPreprocessPath + "MLPacPreprocess_e"+epsilon+".csv";
		this.setupAndGetClassifier(inputDataPath);
		this.setupAttributes();

		// Test the model
		try {
			Evaluation eTest = new Evaluation(this.dataset);
			eTest.evaluateModel(this.classifier, this.dataset);

			// Print the result à la Weka explorer:
			String strSummary = eTest.toSummaryString();
			logger.info(strSummary);

			// Get the confusion matrix
			double[][] cmMatrix = eTest.confusionMatrix();
			String cmStr = "";
			for(int i = 0; i< cmMatrix.length; ++i){
				for(int j=0; j< cmMatrix[i].length; ++j){
					cmStr += " " + cmMatrix[i][j];
				}
				cmStr += "\n";
			}
			logger.info("Confusion Matrix: \n" + cmStr + "\n");
		} catch(Exception e){
			logger.error("Failed to evaluate classifier: " + this.classifier.getClass().getSimpleName(), e);
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

	private void setupAndGetClassifier(String inputDataPath) {
		this.classifier = new J48();
		try {
			this.getInputInstance(inputDataPath);
			logger.info(String.format("Training Dataset shape: instances [%d], features [%d]", dataset.size(), dataset.get(0).numAttributes()));
			this.classifier.buildClassifier(this.dataset);
		} catch (Exception e) {
			logger.error("ERROR initializing classifier: ", e);
		}
	}
	
	

	

	@Override
	public boolean shouldStop(SearchResult incumbentSolution) {

	    //Extract features from an incumbent solution
		Map<PacFeature,Double> features = MLPacFeatureExtractor.extractFeaturesFromSearchResult(incumbentSolution);
		int size = features.size();

		// Init a classifier input instance
		Instance ins = new DenseInstance(size);

        // Add features to the input instance
		int indx = 0;
		for(Entry<PacFeature, Double> entry : features.entrySet()){
			ins.setValue(indx++, entry.getValue());
		}
		ins.setDataset(this.dataset);

		// Create local dataset from the instance
//		Instances localDataSet = new Instances("MlPac",MLPacFeatureExtractor.getAttributes(), 500);
//		localDataSet.setClassIndex(this.dataset.size() -1);
//		localDataSet.add(ins);

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

		logger.debug("distribute  result for instance: [" + ins.toString() +"] is ["+ distributeResult[0] +"]");
		logger.debug("classification  result for instance: [" + ins.toString() +"] is ["+ classificationResult +"]");
		boolean pacConditionResult = distributeResult[0] >= (1-this.delta);
		return pacConditionResult;
	}

}
