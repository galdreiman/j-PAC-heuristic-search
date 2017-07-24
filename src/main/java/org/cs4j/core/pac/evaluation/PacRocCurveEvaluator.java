package org.cs4j.core.pac.evaluation;

import org.apache.log4j.Logger;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * Created by Gal Dreiman on 19/07/2017.
 */
public class PacRocCurveEvaluator {

    final static Logger logger = Logger.getLogger(PacRocCurveEvaluator.class);


    public static void main(String[] args) throws Exception {

        double[] epsilons = {0.0,0.05,0.1,0.2,0.3};

        for(double epsilon : epsilons) {
            // load data
            Instances data = ConverterUtils.DataSource.read("C:\\Users\\user\\workspace\\PAC\\PACBurlap\\PacDaniel\\preprocessResults\\GridPathFinding\\100K_training\\MLPacPreprocess_e"+epsilon+".arff");
            data.setClassIndex(data.numAttributes() - 1);

            // evaluate classifier
            Classifier cl = new J48();
            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(cl, data, 10, new Random(1));


            logger.info("--------------------" + epsilon +"-----------------------");
            logger.info(eval.toSummaryString());
            logger.info(eval.toMatrixString());
            logger.info("-----------------------------------------------");

            // generate curve
            ThresholdCurve tc = new ThresholdCurve();
            int classIndex = 0;
            Instances curve = tc.getCurve(eval.predictions(), classIndex);

            // plot curve
            try {
                ThresholdVisualizePanel tvp = new ThresholdVisualizePanel();
                tvp.setROCString("(Area under ROC = " +
                        Utils.doubleToString(ThresholdCurve.getROCArea(curve), 4) + ")");
                tvp.setName(curve.relationName());
                PlotData2D plotdata = new PlotData2D(curve);
                plotdata.setPlotName(curve.relationName());
                plotdata.addInstanceNumberAttribute();
                // specify which points are connected
                boolean[] cp = new boolean[curve.numInstances()];
                for (int n = 1; n < cp.length; n++)
                    cp[n] = true;
                plotdata.setConnectPoints(cp);
                // add plot
                tvp.addPlot(plotdata);


                // display curve
                final JFrame jf = new JFrame("WEKA ROC: " + tvp.getName());
                jf.setSize(500, 400);
                jf.getContentPane().setLayout(new BorderLayout());
                jf.getContentPane().add(tvp, BorderLayout.CENTER);
                jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                jf.setVisible(false);
            } catch (Exception e) {
                logger.error("Error with native zip: " + e.getMessage());
            }
        }
    }
}
