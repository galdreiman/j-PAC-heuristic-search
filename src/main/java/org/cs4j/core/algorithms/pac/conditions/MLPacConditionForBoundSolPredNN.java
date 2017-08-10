package org.cs4j.core.algorithms.pac.conditions;

import org.apache.log4j.Logger;
import org.cs4j.core.MLPacFeatureExtractor;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess;
import org.cs4j.core.mains.DomainExperimentData;
import weka.classifiers.AbstractClassifier;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

/**
 * Created by Gal Dreiman  on 05/08/2017.
 */
public class MLPacConditionForBoundSolPredNN extends  MLPacConditionForBoundSolPred{

    private final static Logger logger = Logger.getLogger(MLPacConditionForBoundSolPredNN.class);

    static {
        MLPacCondition.clsType = "NN";
    }

    @Override
    public void setup(SearchDomain domain, double epsilon, double delta) {
        super.setup(domain, epsilon, delta);

        // read ML_PAC_Condition_Preprocess.csv and train the model (the output
        // of the training process)
        String inputModelPath = DomainExperimentData.get(domain.getClass(),
                DomainExperimentData.RunType.TRAIN).outputPreprocessPathFormat + "MLPacPreprocess_e"+epsilon+"_"+ this.clsType+".model";
        String inputDataPath = DomainExperimentData.get(domain.getClass(),
                DomainExperimentData.RunType.TRAIN).outputPreprocessPathFormat + "MLPacPreprocess_e"+epsilon+".arff";
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

}
