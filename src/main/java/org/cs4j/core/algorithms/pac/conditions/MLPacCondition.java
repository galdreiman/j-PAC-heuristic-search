package org.cs4j.core.algorithms.pac.conditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.cs4j.core.MLPacFeatureExtractor;
import org.cs4j.core.MLPacFeatureExtractor.PacFeature;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess;
import org.cs4j.core.experiments.MLPacExperiment;
import org.cs4j.core.mains.DomainExperimentData;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
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

	    //Extract features from an incumbent solution
		Map<PacFeature,Double> features = MLPacFeatureExtractor.extractFeaturesFromSearchResult(incumbentSolution);
		int size = features.size();

		// Init a classifier input instance
		Instance ins = new DenseInstance(size + 1);

        // Add features to the input instance
		int indx = 0;
//		for(Entry<PacFeature, Double> entry : features.entrySet()){
//			ins.setValue(indx++, entry.getValue());
//		}

		double generated = features.get(PacFeature.GENERATED);
		ins.setValue(indx++,generated);
		double expanded = features.get(PacFeature.EXPANDED);
		ins.setValue(indx++,expanded);
		double reopened = features.get(PacFeature.ROPENED);
		ins.setValue(indx++,reopened);
		double U = features.get(PacFeature.COST);
		ins.setValue(indx++,U);
		double g1 = features.get(PacFeature.G_0);
		ins.setValue(indx++,g1);
		double h1 = features.get(PacFeature.H_0);
		ins.setValue(indx++,h1);
		double g2 = features.get(PacFeature.G_2);
		ins.setValue(indx++,g2);
		double h2 = features.get(PacFeature.H_2);
		ins.setValue(indx++,h2);
		double g3 = features.get(PacFeature.G_2);
		ins.setValue(indx++,g3);
		double h3 = features.get(PacFeature.H_2);
		ins.setValue(indx++,h3);
		double w = 1.0 + (Double) incumbentSolution.getExtras().get("epsilon");
		ins.setValue(indx++,w);

		logger.info("instance to classify: "+ins.toString());


		this.dataset.setClassIndex(this.dataset.numAttributes() - 1);
		ins.setDataset(this.dataset);

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
		boolean pacConditionResult = distributeResult[1] >= (1-this.delta);
		return pacConditionResult;
	}

}
