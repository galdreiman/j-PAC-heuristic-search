package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.algorithms.DP;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.domains.*;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Roni Stern on 28/02/2017.
 *
 * A utility runner to set up what to run on the server for PAC research
 */
public class PACExperimentRunner {
    final static Logger logger = Logger.getLogger(PACExperimentRunner.class);


    public static void main(String[] args) throws ClassNotFoundException {
        Class[] domains=new Class[]{VacuumRobot.class, DockyardRobot.class, FifteenPuzzle.class};
        if(args.length>1){
            Class domainClass= Class.forName(args[1]);
            domains = new Class[]{domainClass};
        }

        if(args[0].equals("Collect")) {
            logger.info("****************************** collecting stats for open based ");
            collectStatisticsForOpenBased(domains);
        }
        if(args[0].equals("Run")) {
            logger.info("****************************** running threshold based ");
            runThresholdBasedConditions(domains,false);
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

    private static void runThresholdBasedConditions(Class[] domains, boolean withDPS) {
        // Run trivial and ratio-based on all domains
        Class[] pacConditions = { TrivialPACCondition.class, RatioBasedPACCondition.class, FMinCondition.class };
        double[] epsilons = { 1, 0.75, 0.5, 0.25, 0.1,0};// ,1 ,1.5};
        double[] deltas = { 0, 0.1, 0.25, 0.5, 0.75, 0.8, 1 };
        SortedMap<String, String> domainParams = new TreeMap<>();
        SortedMap<String, Object> runParams = new TreeMap<>();
        OutputResult output = null;

        runParams.put("epsilon", -1);
        runParams.put("delta", -1);
        runParams.put("pacCondition", -1);
        Class anytimeSearchClass = AnytimePTS4PAC.class;

        SearchAlgorithm pacSearch = new PACSearchFramework();
        pacSearch.setAdditionalParameter("anytimeSearch", anytimeSearchClass.getName());
        PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();

        for (Class domainClass : domains) {
            logger.info("Running anytime for domain " + domainClass.getName());
            try {
                // Prepare experiment for a new domain
                output = new OutputResult(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TEST).outputPath, "PAC", -1, -1, null, false,
                        true);
                runParams.put("anytimeSearch", anytimeSearchClass.getSimpleName());
                runner.printResultsHeaders(output, runParams);

                PACUtils.loadPACStatistics(domainClass);
                for (Class pacConditionClass : pacConditions) {
                    runParams.put("pacCondition", pacConditionClass.getSimpleName());
                    for (double epsilon : epsilons) {
                        runParams.put("epsilon", epsilon);
                        for (double delta : deltas) {
                            runParams.put("delta", delta);
                            pacSearch.setAdditionalParameter("epsilon", "" + epsilon);
                            pacSearch.setAdditionalParameter("delta", "" + delta);
                            pacSearch.setAdditionalParameter("pacCondition", pacConditionClass.getName());
                            runner.run(domainClass, pacSearch, DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TEST).inputPath, output,
                                    DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TEST).fromInstance,
                                    DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TEST).toInstance, domainParams, runParams);
                        }
                    }
                }

                if(withDPS==true) {
                    // Run DPS on the same epsilon values
                    runParams.put("delta", -1);
                    SearchAlgorithm dps = new DP("DPS", false, false, false); // A
                    // bounded-suboptimal
                    // algorithm
                    runParams.put("searcher", dps.getClass().getSimpleName());
                    for (double epsilon : epsilons) {
                        runParams.put("epsilon", epsilon);
                        dps.setAdditionalParameter("weight", "" + (1 + epsilon));
                        runner.run(domainClass, dps, DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TEST).inputPath, output,
                                DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TEST).fromInstance,
                                DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TEST).toInstance, domainParams, runParams);
                    }
                }

            } catch (IOException e) {
                logger.error(e);
            } finally {
                if (output != null)
                    output.close();
            }
        }
    }
}
