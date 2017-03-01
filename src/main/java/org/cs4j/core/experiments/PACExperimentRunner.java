package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.algorithms.DP;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.domains.*;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Roni Stern on 28/02/2017.
 *
 * A utility runner to set up what to run on the server for PAC research
 */
public class PACExperimentRunner {
    final static Logger logger = Logger.getLogger(PACExperimentRunner.class);


    //@TODO: Replace all this with better handling of command line using some known code to do so
    private static Class[] getClassesFromCommandLine(String[] args){
        // Default classes
        boolean classesFound=false;
        int i;
        for(i=0;i<args.length;i++){
            if(args[i].equals("-classes")) {
                classesFound=true;
                break;
            }
        }
        if(classesFound==false)
            return new Class[]{DockyardRobot.class, Pancakes.class,FifteenPuzzle.class};

        List<Class> domainsList = new ArrayList<>();
        Class domainClass=null;
        for(int j=i+1;j<args.length;j++){
            if(args[j].startsWith("-")) { // Next type of parameter
                return domainsList.toArray(new Class[]{});
            }
            else{
                try {
                    domainClass=Class.forName(args[j]);
                } catch (ClassNotFoundException e) {
                    logger.error("Class "+args[j] + " unknown");
                    domainClass=null;
                }
                if(domainClass!=null)
                    domainsList.add(domainClass);
            }
        }
        return domainsList.toArray(new Class[]{});
    }

    private static Class[] getPACConditionsFromCommandLine(String[] args){
        // Default classes
        boolean classesFound=false;
        int i;
        for(i=0;i<args.length;i++){
            if(args[i].equals("-PACCondition")) {
                classesFound=true;
                break;
            }
        }
        if(classesFound==false)
            return new Class[]{TrivialPACCondition.class, ThresholdPACCondition.class,FMinCondition.class};

        List<Class> pacConditionsList = new ArrayList<>();
        Class domainClass=null;
        for(int j=i+1;j<args.length;j++){
            if(args[j].startsWith("-")) { // Next type of parameter
                return pacConditionsList.toArray(new Class[]{});
            }
            else{
                try {
                    domainClass=Class.forName(args[j]);
                } catch (ClassNotFoundException e) {
                    logger.error("Class "+args[j] + " unknown");
                    domainClass=null;
                }
                if(domainClass!=null)
                    pacConditionsList.add(domainClass);
            }
        }
        return pacConditionsList.toArray(new Class[]{});
    }

    public static void main(String[] args) throws ClassNotFoundException {
        Class[] domains=getClassesFromCommandLine(args);
        Class[] pacConditions = getPACConditionsFromCommandLine(args);

        if(args[0].equals("Collect")) {
            logger.info("****************************** collecting stats for open based ");
            collectStatisticsForOpenBased(domains);
        }
        if(args[0].equals("Run")) {
            logger.info("****************************** running threshold based ");
            runThresholdBasedConditions(domains,pacConditions);
        }

        if(args[0].equals("RunOracle")) {
            logger.info("****************************** running threshold based ");
            runOracleCondition(domains);
        }

        if(args[0].equals("RunFMin")){
            logger.info("****************************** running f-min ");
            runFMinConditions(domains);
        }
    }



    private static void collectStatisticsForOpenBased(Class[] domains) {
        OutputResult output=null;
        StatisticsGenerator generator = new StatisticsGenerator();

        for(Class domainClass : domains) {
            logger.info("Running anytime for domain " + domainClass.getName());
            try {
                // Prepare experiment for a new domain
                output = new OutputResult(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TRAIN).outputPath,
                        "StatisticsGenerator", -1, -1, null, false, true);
                generator.printResultsHeaders(output,
                        new String[]{"InstanceID", "h", "opt"},
                        new TreeMap<>());
                generator.run(domainClass,output,new TreeMap<>(),new TreeMap<>());
                output.close();
            }catch(IOException e){
                logger.error(e);
            }finally{
                if(output!=null)
                    output.close();
            }
        }
    }

    private static void runThresholdBasedConditions(Class[] domains,Class[] pacConditions) {
        // Run trivial and ratio-based on all domains
        double[] epsilons = { 1, 0.75, 0.5, 0.25, 0.1,0};// ,1 ,1.5};
        double[] deltas = { 0, 0.1, 0.25, 0.5, 0.75, 0.8, 1 };

        PACSearchFramework psf = new PACSearchFramework();
        psf.setAnytimeSearchAlgorithm(new AnytimePTS4PAC());
        Experiment experiment = new StandardExperiment(psf);

        PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
        runner.runExperimentBatch(domains,pacConditions,epsilons,deltas,experiment);
    }

    private static void runOracleCondition(Class[] domains) {
        // Run trivial and ratio-based on all domains
        double[] epsilons = { 1, 0.75, 0.5, 0.25, 0.1,0};// ,1 ,1.5};
        double[] deltas = { 0 };
        Class[] pacConditions = new Class[]{OraclePACCondition.class};
        Experiment experiment = new OracleExperiment();

        PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
        runner.runExperimentBatch(domains,pacConditions,epsilons,deltas,experiment);
    }

    private static void runFMinConditions(Class[] domains) {
        // Run trivial and ratio-based on all domains
        double[] epsilons = { 1, 0.75, 0.5, 0.25, 0.1,0};// ,1 ,1.5};
        double[] deltas = { 0 };
        Class[] pacConditions = new Class[]{FMinCondition.class};

        PACSearchFramework psf = new PACSearchFramework();
        psf.setAnytimeSearchAlgorithm(new AnytimePTS4PAC());
        Experiment experiment = new StandardExperiment(psf);

        PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
        runner.runExperimentBatch(domains,pacConditions,epsilons,deltas,experiment);
    }
}
