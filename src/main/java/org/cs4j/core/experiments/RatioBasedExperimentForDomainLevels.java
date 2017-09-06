package org.cs4j.core.experiments;

import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.pac.PACStatistics;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.pac.conf.PacConfig;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by user on 06/09/2017.
 */
public class RatioBasedExperimentForDomainLevels extends PACOnlineExperimentRunner {

    /**
     * Run a batch of experiments
     *
     * @param domains       A set of domain classes
     * @param pacConditions a set of PAC condition classes
     * @param epsilons      possible epsilon values
     * @param deltas        possible delta values
     * @param experiment    an experiment runner object
     */
    @Override
    public void runExperimentBatch(Class[] domains,
                                   Class[] pacConditions,
                                   double[] epsilons,
                                   double[] deltas,
                                   Experiment experiment) {

        SortedMap<String, String> domainParams = new TreeMap<>();
        SortedMap<String, Object> runParams = new TreeMap<>();
        OutputResult output = null;
        runParams.put("epsilon", -1);
        runParams.put("delta", -1);
        runParams.put("pacCondition", -1);


        int[] domainLevels = PacConfig.instance.PredictiondomainLevelsForRatioBased();

        for (Class domainClass : domains) {
            for (int domainLevel : domainLevels) {
                logger.info("Running anytime for domain " + domainClass.getName());
                try {

                    runParams.put("domainLevel", domainLevel);

                    // Prepare experiment for a new domain
                    String domainName = domainClass.getSimpleName();
                    output = new OutputResult(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TEST).outputOnlinePath, domainName + File.separator + "PAC_Output_RB-"+ domainLevel+"_" + experiment.getClass().getSimpleName(), -1, -1, null, false,
                            true);
                    this.printResultsHeaders(output, experiment.getResultsHeaders(), runParams);


                    String statisticsFile = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TRAIN).inputPathFormat, domainLevel)
                            + File.separator + PACStatistics.STATISTICS_FILE_NAME;
                    PACStatistics statistics = PACUtils.parsePACStatisticsFile(statisticsFile);
                    PACUtils.setPacStatistics(domainClass, statistics);


                    for (Class pacConditionClass : pacConditions) {
                        runParams.put("pacCondition", pacConditionClass.getSimpleName());
                        for (double epsilon : epsilons) {
                            runParams.put("epsilon", epsilon);
                            for (double delta : deltas) {
                                runParams.put("delta", delta);
                                runParams.put("pacCondition", pacConditionClass.getName());
                                this.run(experiment, domainClass, DomainExperimentData.RunType.TEST, output, // TODO: GAL: Dont forget to retrieve to TEST!!!!!!
                                        domainParams, runParams);
                            }
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

    public void run(Experiment experiment,
                    Class domainClass,
                    DomainExperimentData.RunType runType,
                    OutputResult output,
                    SortedMap<String, String> domainParams,
                    SortedMap<String, Object> runParams) {
        SearchDomain domain;

        int domainLevel = Integer.parseInt(runParams.get("domainLevel").toString());

        Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
        logger.info("Solving " + domainClass.getName());
        int fromInstance = DomainExperimentData.get(domainClass, runType).fromInstance;
        int toInstance = DomainExperimentData.get(domainClass, runType).toInstance;
        String inputPath = String.format(DomainExperimentData.get(domainClass, runType).inputPathFormat,domainLevel);

        // search on this domain and algo and weight the 100 instances
        for (int i = fromInstance; i <= toInstance; ++i) {
            logger.info("\rSolving " + domainClass.getName() + "\t instance " + i + "\t" + runParamsToLog(runParams));
            domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
            experiment.run(domain, output, i, runParams);
        }
    }
}
