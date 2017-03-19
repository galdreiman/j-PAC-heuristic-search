package org.cs4j.core.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.algorithms.pac.AnytimePTS4PAC;
import org.cs4j.core.algorithms.pac.FMinCondition;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.SearchAwarePACSearchImpl;
import org.cs4j.core.algorithms.pac.conditions.BoundedCostPACSearch;
import org.cs4j.core.algorithms.pac.conditions.OpenBasedPACCondition;
import org.cs4j.core.algorithms.pac.conditions.OraclePACCondition;
import org.cs4j.core.algorithms.pac.conditions.RatioBasedPACCondition;
import org.cs4j.core.algorithms.pac.conditions.TrivialPACCondition;
import org.cs4j.core.algorithms.pac.preprocess.StatisticsGenerator;
import org.cs4j.core.domains.DockyardRobot;
import org.cs4j.core.domains.FifteenPuzzle;
import org.cs4j.core.domains.GridPathFinding;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.domains.VacuumRobot;
import org.cs4j.core.mains.DomainExperimentData;

/**
 * Created by Roni Stern on 28/02/2017.
 *
 * A utility runner to set up what to run on the server for PAC research
 */
public class PACExperimentRunner {
    private final static Logger logger = Logger.getLogger(PACExperimentRunner.class);
    private final static double[] DEFAULT_EPSILONS = new double[] { 1.0, 0.75, 0.5, 0.25, 0.1,0.0};
    private final static double[] DEFAULT_DELTAS = new double[] { 1,0.8,0.75,0.5,0.25,0.1,0.0 };
    private final static Class[] DEFAULT_CLASSES = new Class[]{DockyardRobot.class, GridPathFinding.class, Pancakes.class,VacuumRobot.class,FifteenPuzzle.class, };

    //@TODO: Replace all this with better handling of command line using some known code to do so
    private Class[] getClassesFromCommandLine(String[] args){
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
            return DEFAULT_CLASSES;

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

    /**
     * Read epsilon valuse from command line
     */
    private double[] getEpsilonValuesFromCommandLine(String[] args){
        // Default classes
        boolean epislonsFound=false;
        int i;
        for(i=0;i<args.length;i++){
            if(args[i].equals("-epsilon")) {
                epislonsFound=true;
                break;
            }
        }
        if(epislonsFound==false)
            return DEFAULT_EPSILONS;

        List<Double> epsilonList = new ArrayList<>();
        Double epsilon=null;
        for(int j=i+1;j<args.length;j++){
            if(args[j].startsWith("-")) { // Next type of parameter
                break; // Moved to next parameter- return the epsilons
            }
            else{
                try {
                    epsilon = Double.parseDouble(args[j]);
                } catch (NumberFormatException e) {
                    logger.error("Value "+args[j] + " is not Double");
                    throw new RuntimeException(e);
                }

                epsilonList.add(epsilon);
            }
        }
        double[] epsilons = new double[epsilonList.size()];
                for(i=0;i<epsilonList.size();i++)
                    epsilons[i]=epsilonList.get(i);
        return epsilons;
    }

    private Class[] getPACConditionsFromCommandLine(String[] args){
        // Default classes
        boolean classesFound=false;
        int i;
        for(i=0;i<args.length;i++){
            if(args[i].equals("-pacCondition")) {
                classesFound=true;
                break;
            }
        }
        if(classesFound==false)
            return new Class[]{TrivialPACCondition.class, RatioBasedPACCondition.class,FMinCondition.class};

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

    private void collectStatisticsForOpenBased(Class[] domains) {
        OutputResult output=null;
        StatisticsGenerator generator = new StatisticsGenerator();

        for(Class domainClass : domains) {
            logger.info("Running anytime for domain " + domainClass.getName());
            try {
                // Prepare experiment for a new domain
                output = new OutputResult(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TRAIN).outputPath,
                        "openBasedStatistics", -1, -1, null, false, true);
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

    /**
     * Collects statistics for the threshold based  PAC conditions
     * @param domains list of classes
     */
    private void collectStatisticsForThresholdBased(Class[] domains){
        OutputResult output=null;
        PacPreprocessRunner generator = new PacPreprocessRunner();
        HashMap<String, String> domainParams = new HashMap<>();
        for(Class domainClass : domains) {
            logger.info("Running anytime for domain " + domainClass.getName());
            try {
                // Prepare experiment for a new domain
                output = new OutputResult(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TRAIN).outputPath,
                        "ThesholdBasedStatisticsGenerator", -1, -1, null, false, true);
                generator.run(domainClass,domainParams);
                output.close();
            }catch(IOException e){
                logger.error(e);
            }finally{
                if(output!=null)
                    output.close();
            }
        }
    }

    private void runThresholdBasedConditions(Class[] domains,Class[] pacConditions,double[] epsilons) {
        // Run trivial and ratio-based on all domains
        PACSearchFramework psf = new PACSearchFramework();
        psf.setAdditionalParameter("anytimeSearch", AnytimePTS4PAC.class.getName());
        Experiment experiment = new StandardExperiment(psf);

        PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
        runner.runExperimentBatch(domains,pacConditions,epsilons, DEFAULT_DELTAS,experiment);
    }

    private void runBoundedCostBased(Class[] domains,double[] epsilons) {
        // Run trivial and ratio-based on all domains
        Class[] pacConditions = new Class[]{TrivialPACCondition.class, RatioBasedPACCondition.class};

        PACSearchFramework psf = new PACSearchFramework();
        psf.setAdditionalParameter("anytimeSearch", BoundedCostPACSearch.class.getName());
        Experiment experiment = new StandardExperiment(psf);

        PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
        runner.runExperimentBatch(domains,pacConditions,epsilons, DEFAULT_DELTAS,experiment);
    }

    private void runOracleCondition(Class[] domains,double[] epsilons) {
        // Run trivial and ratio-based on all domains
        double[] deltas = { 0 };
        Class[] pacConditions = new Class[]{OraclePACCondition.class};
        Experiment experiment = new OracleExperiment();

        PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
        runner.runExperimentBatch(domains,pacConditions,epsilons,deltas,experiment);
    }

    private void runOpenBased(Class[] domains,double[] epsilons, double[] deltas) {
        // Run trivial and ratio-based on all domains
        Class[] pacConditions = new Class[]{OpenBasedPACCondition.class};
        PACSearchFramework psf = new PACSearchFramework();
        psf.setAnytimeSearchClass(SearchAwarePACSearchImpl.class);
        Experiment experiment = new OpenBasedExperiment();

        PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
        runner.runExperimentBatch(domains,pacConditions,epsilons,deltas,experiment);
    }

    private void runFMinConditions(Class[] domains,double[] epsilons) {
        // Run trivial and ratio-based on all domains
        double[] deltas = { 0 };
        Class[] pacConditions = new Class[]{FMinCondition.class};

        PACSearchFramework psf = new PACSearchFramework();
        psf.setAdditionalParameter("anytimeSearch", AnytimePTS4PAC.class.getName());
        Experiment experiment = new StandardExperiment(psf);

        PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
        runner.runExperimentBatch(domains,pacConditions,epsilons,deltas,experiment);
    }


    private void runDPS(Class[] domains,double[] epsilons){
        // Run trivial and ratio-based on all domains
        DPSExperimentRunner runner = new DPSExperimentRunner();
        runner.runExperimentBatch(domains,epsilons);
    }


    public static void main(String[] args) throws ClassNotFoundException {
        PACExperimentRunner runner = new PACExperimentRunner();

        Class[] domains=runner.getClassesFromCommandLine(args);
        Class[] pacConditions = runner.getPACConditionsFromCommandLine(args);
        double[] epsilonValues = runner.getEpsilonValuesFromCommandLine(args);
        if(args[0].equals("RunOpenBased")){
            logger.info("********** OPEN BASED CONDITION");
            runner.runOpenBased(domains,epsilonValues,DEFAULT_DELTAS);
        }

        if(args[0].equals("BoundedCostBased")){
            logger.info("********** Bounded Cost Based Search");
            runner.runBoundedCostBased(domains,epsilonValues);
        }


        if(args[0].equals("CollectOpenBased")) {
            logger.info("****************************** collecting stats for open based ");
            runner.collectStatisticsForOpenBased(domains);
        }
        if(args[0].equals("CollectThresholdBased")) {
            logger.info("****************************** collecting stats for threshold based ");
            runner.collectStatisticsForThresholdBased(domains);
        }
        if(args[0].equals("Run")) {
            logger.info("****************************** running threshold based ");
            runner.runThresholdBasedConditions(domains,pacConditions,epsilonValues);
        }

        if(args[0].equals("RunOracle")) {
            logger.info("****************************** running Oracle ");
            runner.runOracleCondition(domains,epsilonValues);
        }

        if(args[0].equals("RunFMin")){
            logger.info("****************************** running f-min ");
            runner.runFMinConditions(domains,epsilonValues);
        }

        if(args[0].equals("RunDPS")){
            logger.info("****************************** running DPS ");
            runner.runDPS(domains,epsilonValues);
        }

    }
}
