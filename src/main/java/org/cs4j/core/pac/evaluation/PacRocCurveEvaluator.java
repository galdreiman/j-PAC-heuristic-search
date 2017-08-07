package org.cs4j.core.pac.evaluation;


import org.apache.log4j.Logger;
import org.cs4j.core.domains.DockyardRobot;
import org.cs4j.core.domains.GridPathFinding;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.domains.VacuumRobot;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.trees.J48;
import weka.core.*;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVSaver;
import weka.core.converters.ConverterUtils;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by Gal Dreiman on 19/07/2017.
 */
public class PacRocCurveEvaluator {

    final static Logger logger = Logger.getLogger(PacRocCurveEvaluator.class);


    public static void main(String[] args) throws Exception {

        double[] epsilons = {0.0,0.05,0.1,0.2,0.3};
        Class[] domains = {DockyardRobot.class}; //{VacuumRobot.class, Pancakes.class, GridPathFinding.class};

        ArrayList<String> outputTable = new ArrayList<>();

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
        headerTable += ",Delta,Epsilon,AUC";
        outputTable.add(headerTable);



        for(Class domain : domains) {
            for (double epsilon : epsilons) {
                // load data
                String inputArffFile = "C:\\Users\\user\\Documents\\Gal\\PAC\\ML-Trained-Data\\TenK\\MLPacPreprocess_e" + epsilon +"_"+ domain.getSimpleName()+ ".arff";
                Instances data = ConverterUtils.DataSource.read(inputArffFile);
                data.setClassIndex(data.numAttributes() - 1);

                // evaluate classifier
                Classifier cl = new J48();
                Evaluation eval = new Evaluation(data);
                eval.crossValidateModel(cl, data, 10, new Random(1));


                logger.info("--------------------" + epsilon + "-----------------------");
                logger.info(eval.toSummaryString());
                logger.info(eval.toMatrixString());
                logger.info("-----------------------------------------------");

                // generate curve
                ThresholdCurve tc = new ThresholdCurve();
                int classIndex = 0;
                Instances curve = tc.getCurve(eval.predictions(), classIndex);


                for(int i = 0; i < curve.numInstances(); ++i){
                    int size = fv.size();

                    ArrayList<String> row = new ArrayList<>();

                    for(int insIndx = 0; insIndx < curve.get(i).numAttributes(); ++insIndx){
                        row.add(curve.get(i).value(insIndx) +"");
                    }
                    row.add(domain.getSimpleName());
                    row.add(""+epsilon);
                    row.add(""+ThresholdCurve.getROCArea(curve));

                    outputTable.add(row.stream().collect(Collectors.joining(",")));

                }


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
//                    final JFrame jf = new JFrame("WEKA ROC: " + tvp.getName());
//                    jf.setSize(500, 400);
//                    jf.getContentPane().setLayout(new BorderLayout());
//                    jf.getContentPane().add(tvp, BorderLayout.CENTER);
//                    jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//                    jf.setVisible(true);
                } catch (Exception e) {
                    logger.error("Error with native zip: " + e.getMessage());
                }
            }
        }


        saveInstancesToFile(outputTable);
    }

    private static void saveInstancesToFile( ArrayList<String> outputTable) throws IOException {
        String outputCsvFile = "C:\\Users\\user\\Documents\\Gal\\PAC\\ML-Trained-Data\\TenK\\Out_MLPacPreprocess_Evaluation.csv";

        String table = outputTable.stream().collect(Collectors.joining("\n"));

        try(  PrintWriter out = new PrintWriter( outputCsvFile )  ){
            out.println( table );
        }
    }
}
