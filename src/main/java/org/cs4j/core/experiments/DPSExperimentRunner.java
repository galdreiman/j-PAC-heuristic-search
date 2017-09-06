package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.DP;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Roni Stern on 04/03/2017.
 *
 * Designed for running a batch of DPS experiments.
 */
public class DPSExperimentRunner {
    final static Logger logger = Logger.getLogger(DPSExperimentRunner.class);

    /**
     * Print the headers for the experimental results into the output file
     *
     * @param output
     * @param runParams
     * @throws IOException
     */
    public void printResultsHeaders(OutputResult output, String[] defaultColumnNames,SortedMap<String, Object> runParams) throws IOException {
        List<String> runParamColumns = new ArrayList<>(runParams.keySet());
        List<String> columnNames = new ArrayList();
        for (String columnName : defaultColumnNames)
            columnNames.add(columnName);
        columnNames.addAll(runParamColumns);
        columnNames.add("Domain");
        columnNames.add("OPT");
        columnNames.add("is_epsilon");
        String toPrint = String.join(",", columnNames);
        output.writeln(toPrint);
    }

    /**
     * Run a batch of experiments
     * @param domains A set of domain classes
     * @param epsilons possible epsilon values
     */
    public void runExperimentBatch(Class[] domains,
                                   double[] epsilons) {
        SortedMap<String, String> domainParams = new TreeMap<>();
        SortedMap<String, Object> runParams = new TreeMap<>();
        OutputResult output = null;
        runParams.put("weight", -1);
        Experiment experiment = new DPSExperiment();
        for (Class domainClass : domains) {
            logger.info("Running DPS for domain " + domainClass.getName());
            try {
                // Prepare experiment for a new domain
                output = new OutputResult(DomainExperimentData.get(domainClass,
                        DomainExperimentData.RunType.TEST).outputOnlinePath, "DPS", -1, -1, null, false,true);
                this.printResultsHeaders(output, experiment.getResultsHeaders(), runParams);
                for (double epsilon : epsilons) {
                    runParams.put("weight", 1+epsilon);
                    runParams.put("epsilon", epsilon);
                    this.run(experiment,domainClass, DomainExperimentData.RunType.TEST, output,
                                    domainParams, runParams);
                }
            } catch (IOException e) {
                logger.error(e);
            } finally {
                if (output != null)
                    output.close();
            }
        }
    }


    public void run(Experiment experiment,
                    Class domainClass,
                    DomainExperimentData.RunType runType,
                    OutputResult output,
                    SortedMap<String, String> domainParams,
                    SortedMap<String, Object> runParams){
        SearchDomain domain;

        Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
        logger.info("Solving " + domainClass.getName());
        int fromInstance = DomainExperimentData.get(domainClass,runType).fromInstance;
        int toInstance = DomainExperimentData.get(domainClass,runType).toInstance;
        String inputPath = DomainExperimentData.get(domainClass,runType).inputPath;

        // search on this domain and algo and weight the 100 instances
        for (int i = fromInstance; i <= toInstance; ++i) {
            logger.info("\rSolving " + domainClass.getName() + "\t instance " + i + "\t"+runParamsToLog(runParams));
            domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
            experiment.run(domain, output, i, runParams);
        }
    }


    private String runParamsToLog(SortedMap<String, Object> runParams){
        StringBuilder builder = new StringBuilder();
        for(String param : runParams.keySet()){
            builder.append(param);
            builder.append("\t");
            builder.append(runParams.get(param));
        }
        return builder.toString();
    }

}
