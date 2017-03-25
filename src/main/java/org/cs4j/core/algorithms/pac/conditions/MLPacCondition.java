package org.cs4j.core.algorithms.pac.conditions;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.cs4j.core.MLPacFeatureExtractor;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.MLPacFeatureExtractor.PacFeature;
import org.cs4j.core.mains.DomainExperimentData;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

public class MLPacCondition extends AbstractPACCondition {

	private final static Logger logger = Logger.getLogger(MLPacCondition.class);

	private AbstractClassifier classifier;
	

	@Override
	public void setup(SearchDomain domain, double epsilon, double delta) {
		super.setup(domain, epsilon, delta);

		// read ML_PAC_Condition_Preprocess.csv and train the model (the output
		// of the training process)
		String inputDataPath = DomainExperimentData.get(domain.getClass(),
				DomainExperimentData.RunType.TRAIN).outputPath;
		this.setupAndGetClassifier(inputDataPath);
	}

	private Instances getInputInstance(String inputDataPath) {
		logger.debug("getInputInstance | input file: " + inputDataPath);
		CSVLoader loader = new CSVLoader();
		Instances data = null;
		try {
			loader.setSource(new File(inputDataPath));
			data = loader.getDataSet();
		} catch (IOException e) {
			logger.error("ERROR: failed to read input data for classifier: " + inputDataPath);
			e.printStackTrace();
		}

		return data;
	}

	private void setupAndGetClassifier(String inputDataPath) {
		String[] options = { "-U" };
		this.classifier = new J48();
		try {
			this.classifier.setOptions(options);
			this.classifier.buildClassifier(this.getInputInstance(inputDataPath));
		} catch (Exception e) {
			logger.error("ERROR initializing classifier: ", e);
			e.printStackTrace();
		}
	}
	
	

	

	@Override
	public boolean shouldStop(SearchResult incumbentSolution) {
		
		Map<PacFeature,Double> features = MLPacFeatureExtractor.extractFeaturesFromSearchResult(incumbentSolution);
		int size = features.size();
		
		Instance ins = new DenseInstance(size);
		
		int indx = 0;
		for(Entry<PacFeature, Double> entry : features.entrySet()){
			ins.setValue(indx++, entry.getValue());
		}
		
		double classResult = -1;
		try {
			classResult = this.classifier.classifyInstance(ins);
		} catch (Exception e) {
			logger.error("ERROR: Failed to classify instance: " + ins.toString());
			e.printStackTrace();
		}
		
		logger.debug("Classification result for instance: [" + ins.toString() +"] is ["+ classResult +"]");
		return classResult >= (1-this.delta);
	}

}
