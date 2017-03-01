package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.IDAstar;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.algorithms.pac.SearchAwarePACCondition;
import org.cs4j.core.domains.*;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Roni Stern on 01/03/2017.
 */
public class FindOptimalsRunner {
    final static Logger logger = Logger.getLogger(FindOptimalsRunner.class);

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
            // Only solve for instances we can solve optimally @TODO: Make this a parameter to pass on
            if(PACUtils.getOptimalSolutions(domainClass).containsKey(i)==false) {
                //@
                logger.info("Skipping instance "+i+" because we don't have an optimal solution for it");
                continue;
            }

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

    /**
     * Print the headers for the experimental results into the output file
     *
     * @param output
     * @param runParams
     * @throws IOException
     */
    public void printResultsHeaders(OutputResult output, SortedMap<String, Object> runParams) throws IOException {
        String[] defaultColumnNames = new String[] { "InstanceID", "Found", "Depth", "Cost", "Iterations", "Generated",
                "Expanded", "Cpu Time", "Wall Time" };
        List<String> runParamColumns = new ArrayList<>(runParams.keySet());
        List<String> columnNames = new ArrayList();
        for (String columnName : defaultColumnNames)
            columnNames.add(columnName);
        columnNames.addAll(runParamColumns);
        String toPrint = String.join(",", columnNames);
        output.writeln(toPrint);
    }


    /**
     * Run a batch of experiments
     * @param domains A set of domain classes
     * @param experiment an experiment runner object
     */
    public void runExperimentBatch(Class[] domains,
                                   Experiment experiment) {
        SortedMap<String, String> domainParams = new TreeMap<>();
        SortedMap<String, Object> runParams = new TreeMap<>();
        OutputResult output = null;
        for (Class domainClass : domains) {
            logger.info("Running anytime for domain " + domainClass.getName());
            try {
                // Prepare experiment for a new domain
                output = new OutputResult(DomainExperimentData.get(domainClass,
                        DomainExperimentData.RunType.ALL).outputPath,
                        "Optimals", -1, -1, null, false,
                        true);
                this.printResultsHeaders(output, runParams);

                this.run(experiment,domainClass,
                        DomainExperimentData.RunType.ALL,
                        output,
                        domainParams, runParams);
            } catch (IOException e) {
                logger.error(e);
            } finally {
                if (output != null)
                    output.close();
            }
        }
    }

    /**
     * Running PAC experiments
     *
     * @param args
     */
    public static void main(String[] args) {
        //Class[] domains = { Pancakes.class, GridPathFinding.class, VacuumRobot.class, DockyardRobot.class,FifteenPuzzle.class };
        Class[] domains = { FifteenPuzzle.class };
        IDAstar search = new IDAstar();
        Experiment experiment = new StandardExperiment(search){
            @Override
            public void run(SearchDomain instance, OutputResult output, int instanceId, SortedMap<String, Object> runParams) {
                SearchResult result;
                List resultsData;
                try {
                    // Set run params into the search algorithm
                    for(String runParam : runParams.keySet()){
                        if(this.searchAlgorithm.getPossibleParameters().containsKey(runParam))
                            this.searchAlgorithm.setAdditionalParameter(runParam,runParams.get(runParam).toString());
                    }

                    result = searchAlgorithm.search(instance);
                    logger.info("Solution found? " + result.hasSolution());
                    double savedOptimal = PACUtils.getOptimalSolutions(instance.getClass()).get(instanceId);
                    if(result.hasSolution()) {
                        if (result.getBestSolution().getCost() != savedOptimal)
                            logger.info("Instance " + instanceId + " optimal is inconsisent " + savedOptimal + "!=" + result.getBestSolution().getCost());
                        else
                            logger.info("Instance " + instanceId + " optimal is consisent!");
                    }
                    resultsData = new ArrayList<>();
                    resultsData.add(instanceId); // Instance ID
                    appendSearchResults(result, resultsData); // Search results data
                    // (expanded, cpu
                    // time, etc.)
                    resultsData.addAll(runParams.values()); // Parameters that
                    // are constant for
                    // this run (w,
                    // domain, etc.)

                    output.appendNewResult(resultsData.toArray());
                    output.newline();
                } catch (OutOfMemoryError e) {
                    logger.info("OutOfMemory in:" + searchAlgorithm.getName() + " on:" + instance.getClass().getName());
                } catch (IOException e) {
                    logger.info("IOException at instance " + instanceId);
                }
            }
        };
        FindOptimalsRunner runner = new FindOptimalsRunner();
        runner.runExperimentBatch(domains,experiment);
    }

}
