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
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
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

//		for(int i =0; i < data.size(); ++i){
//			data.get(i).deleteAttributeAt(0);
//			data.get(i).deleteAttributeAt(1);
//			data.get(i).deleteAttributeAt(2);
//		}

		this.dataset = data;
		this.dataset.setClassIndex(data.numAttributes() - 1);
	}

	private void setupAndGetClassifier(String inputDataPath) {
		String[] options = { "-U" };
		this.classifier = new J48();
		try {
			this.classifier.setOptions(options);
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
			ins.setDataset(this.dataset);
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

		logger.debug("distribute  result for instance: [" + ins.toString() +"] is ["+ distributeResult[0] +"]");
		logger.debug("classification  result for instance: [" + ins.toString() +"] is ["+ classificationResult +"]");
		boolean pacConditionResult = distributeResult[0] >= (1-this.delta);
		return pacConditionResult;
	}

}
