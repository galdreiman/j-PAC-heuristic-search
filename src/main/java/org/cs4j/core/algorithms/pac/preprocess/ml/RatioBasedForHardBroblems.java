package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.pac.conditions.RatioBasedPACCondition;
import org.cs4j.core.algorithms.pac.preprocess.StatisticsGenerator;
import org.cs4j.core.experiments.Experiment;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.experiments.MLPacExperiment;
import org.cs4j.core.experiments.RatioBasedExperimentForDomainLevels;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.pac.conf.PacConfig;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Created by Gal Dreiman on 06/09/2017.
 */
public class RatioBasedForHardBroblems extends StatisticsGenerator{

    private final static Logger logger = Logger.getLogger(RatioBasedForHardBroblems.class);

    private int domainLevel;

    public static void main(String[] args){

        Class[] domains = PacConfig.instance.pacDomains();
        int[] domainLevels = PacConfig.instance.PredictiondomainLevelsForRatioBased();
        int domainLevelTest = PacConfig.instance.PredictiondomainLevelTestForRatioBased();

        List<Integer> levels = new ArrayList<>();
        for(int d : domainLevels){
            levels.add(d);
        }
        levels.add(domainLevelTest);


        OutputResult output=null;
        RatioBasedForHardBroblems generator = new RatioBasedForHardBroblems();

        for(Class domainClass : domains) {
            for(int level : levels) {
                logger.info("Running anytime for domain " + domainClass.getName());
                try {
                    // Prepare experiment for a new domain
                    generator.setDomainLevel(level);
                    String inputFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).inputPathFormat, level);
                    output = new OutputResult(inputFile,
                            "StatisticsGenerator", -1, -1, null, false, true);
                    generator.printResultsHeaders(output,
                            new String[]{"InstanceID", "h", "opt"},
                            new TreeMap<>());
                    generator.run(domainClass, output, new TreeMap<>(), new TreeMap<>());
                    output.close();
                } catch (IOException e) {
                    logger.error(e);
                } finally {
                    if (output != null)
                        output.close();
                }
            }
        }

        double[] epsilons = PacConfig.instance.inputOnlineEpsilons();
        double[] deltas = PacConfig.instance.inputOnlineDeltas();
        Class[] pacConditions = {RatioBasedPACCondition.class};

        Experiment experiment = new RatioBasedPacExperiment();
        RatioBasedExperimentForDomainLevels runner = new RatioBasedExperimentForDomainLevels();
        runner.runExperimentBatch(domains, pacConditions, epsilons, deltas, experiment);


    }

    public void setDomainLevel(int domainLevel){
        this.domainLevel = domainLevel;
    }

    public void run(Class domainClass,
                    OutputResult output,
                    SortedMap<String, String> domainParams,
                    SortedMap<String,Object> runParams) {
        SearchDomain domain;
        int fromInstance= DomainExperimentData.get(domainClass,DomainExperimentData.RunType.ALL).fromInstance;
        int toInstance= DomainExperimentData.get(domainClass,DomainExperimentData.RunType.ALL).toInstance;
        String inputPath = String.format(DomainExperimentData.get(domainClass,DomainExperimentData.RunType.TRAIN).inputPathFormat, this.domainLevel);
        Map<Double,Double> hToOptimal;
        Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
        try {
            // search on this domain and algo and weight the 100 instances
            for (int i = fromInstance; i <= toInstance; ++i) {
                // Read domain from file
                logger.info("\rGenerating statistics for " + domainClass.getName() + "\t instance " + i);
                domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
                hToOptimal = this.runOnInstance(domain);
                logger.info("Statistics generated!");

                for(Double h : hToOptimal.keySet()) {
                    output.appendNewResult(new Object[]{i,h,hToOptimal.get(h)});
                    output.newline();
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }



}
