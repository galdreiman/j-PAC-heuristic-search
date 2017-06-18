package org.cs4j.core.experiments;

import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by user on 18/06/2017.
 */
public class PACOnlineExperimentRunnerForIncInstances extends PACOnlineExperimentRunner {

    private int fromInstance;
    private int toInstance;
    private int numOfBatches = 5;
    private int currToInstance;

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

        for (Class domainClass : domains) {
            logger.info("Running anytime for domain " + domainClass.getName());
            try {
                // Prepare experiment for a new domain
                String domainName = domainClass.getSimpleName();

                //batch run setup:
                this.fromInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TEST).fromInstance;
                this.toInstance = DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TEST).toInstance;
                int instancesDelta = (this.fromInstance - this.toInstance +1) / this.numOfBatches;

                for(this.currToInstance = this.fromInstance; this.currToInstance <= this.toInstance; this.currToInstance += instancesDelta) {
                    int batchCount = this.currToInstance - this.fromInstance;
                    output = new OutputResult(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TEST).outputOnlinePath, domainName + File.separator + "PAC_Output_"+batchCount, -1, -1, null, false,
                            true);
                    this.printResultsHeaders(output, experiment.getResultsHeaders(), runParams);

                    PACUtils.getPACStatistics(domainClass); // Loads the statistics from the disk
                    for (Class pacConditionClass : pacConditions) {
                        runParams.put("pacCondition", pacConditionClass.getSimpleName());
                        for (double epsilon : epsilons) {
                            runParams.put("epsilon", epsilon);
                            for (double delta : deltas) {
                                runParams.put("delta", delta);
                                runParams.put("pacCondition", pacConditionClass.getName());
                                this.run(experiment, domainClass, DomainExperimentData.RunType.TEST, output,
                                        domainParams, runParams);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.error(e);
                e.printStackTrace();
            } finally {
                if (output != null)
                    output.close();
            }
        }
    }

    @Override
    public void run(Experiment experiment,
                    Class domainClass,
                    DomainExperimentData.RunType runType,
                    OutputResult output,
                    SortedMap<String, String> domainParams,
                    SortedMap<String, Object> runParams) {
        SearchDomain domain;

        Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
        logger.info("Solving " + domainClass.getName());
        String inputPath = DomainExperimentData.get(domainClass, runType).inputPath;

        // search on this domain and algo and weight the 100 instances
        for (int i = this.fromInstance; i <= this.currToInstance; ++i) {
            logger.info("\rSolving " + domainClass.getName() + "\t instance " + i + "\t" + runParamsToLog(runParams));
            domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
            experiment.run(domain, output, i, runParams);
        }
    }
}
