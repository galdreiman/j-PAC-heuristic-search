package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.domains.*;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by user on 26/02/2017.
 */
public class AnytimeExperimentRunner {

    final static Logger logger = Logger.getLogger(AnytimeExperimentRunner.class);


    /**
     * Extract from SearchResults the data to output to the file
     * @param resultsData the results data object to update with data from the SearchResults
     * @param result the SearchResults object to extract from
     */
    protected void setResultsData(double[] resultsData, SearchResult result) {
        resultsData[2] = 1;
        resultsData[3] = result.getBestSolution().getLength();
        resultsData[4] = result.getBestSolution().getCost();
        resultsData[5] = (double)result.getExtras().get("initial-h");
        resultsData[6] = result.getGenerated();
        resultsData[7] = result.getExpanded();
        resultsData[8] = result.getCpuTimeMillis();
        resultsData[9] = result.getWallTimeMillis();
    }

    public void run(Class domainClass, AnytimeSearchAlgorithm alg, String inputPath, String outputPath, int startInstance,
                        int stopInstance, HashMap<String, String> domainParams) {
        String[] resultColumnNames = { "InstanceID", "Iteration", "Found",
                "Depth", "Cost", "initial-h", "Generated", "Expanded", "Cpu Time", "Wall Time" };
        OutputResult output = null;

        double[] resultsData;
        SearchDomain domain;
        int anytimeIteration;
        SearchResultImpl totalResult;
        SearchResult iterationResult;
        int solvableNum = stopInstance - startInstance + 1;
        boolean[] solvableInstances = new boolean[solvableNum];
        Arrays.fill(solvableInstances, true);

        Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
        try {
            // Write headers of output table
            output = new OutputResult(outputPath + alg.getName(), null, -1, -1, null, false, true);
            String toPrint = String.join(",", resultColumnNames);
            output.writeln(toPrint);

            System.out.println("Solving " + domainClass.getName() + "\tAlg: " + alg.getName());
            // search on this domain and algo and weight the 100 instances
            for (int i = startInstance; i <= stopInstance; ++i) {
                try {
                    // Read domain from file
                    resultsData = new double[resultColumnNames.length];
                    resultsData[0]=i;
                    domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);

                    System.out.print("\rSolving " + domainClass.getName() + "\t instance "+ i+"\t Alg: " + alg.getName());

                    if (solvableInstances[i - startInstance]){
                        anytimeIteration=0;
                        iterationResult = (SearchResultImpl)alg.search(domain);
                        while(iterationResult.hasSolution()){
                            // This search results accumulates all expanded and generated so far
                            totalResult = (SearchResultImpl) alg.getTotalSearchResults();
                            resultsData[1]=anytimeIteration;
                            setResultsData(resultsData, totalResult);
                            output.appendNewResult(resultsData);
                            output.newline();
                            iterationResult = (SearchResultImpl) alg.continueSearch();
                            anytimeIteration++;
                        }
                   }else{
                        solvableInstances[i - startInstance] = false;
                        solvableNum--;

                        int sul = 0;
                        for (boolean solvableInstance : solvableInstances) {
                            if (solvableInstance)
                                sul++;
                        }
                        if (sul != solvableNum)
                            System.out.println("[WARNING] solvable num incorrect i:" + i);
                    }
                } catch (OutOfMemoryError e) {
                    logger.info("OutOfMemory in:" + alg.getName() + " on:" + domainClass.getName());
                } catch (FileNotFoundException e) {
                    logger.info("[INFO] FileNotFoundException At inputPath:" + inputPath);
                    i = stopInstance; // @TODO: Is this just a replacement for a break?
                }
            }
        } catch (IOException e) {
            logger.error("[INFO] IOException At outputPath:" + outputPath);
            e.printStackTrace();
        } finally {
            output.close();
        }
    }


    /**
     * This main runs on all domains and output the values returned in every iteration
     * of the anytime search algorithm, along with the initial h values.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        AnytimeExperimentRunner runner = new AnytimeExperimentRunner();
        AnytimeSearchAlgorithm algorithm = new AnytimePTS(){
            @Override
            public SearchResult search(SearchDomain domain) {
                double initialH = domain.initialState().getH();
                SearchResult results = super.search(domain);
                results.getExtras().put("initial-h",initialH);
                return results;
            }};
        HashMap domainParams = new HashMap<>();

        Class[] domains = {GridPathFinding.class, DockyardRobot.class, FifteenPuzzle.class,Pancakes.class, VacuumRobot.class};
        for(Class domainClass : domains) {
            logger.info("Running anytime for class "+domainClass.getName());
            runner.run(domainClass, algorithm,
                    DomainExperimentData.get(domainClass,RunType.TEST).inputPath,
                    DomainExperimentData.get(domainClass,RunType.TEST).outputOnlinePath,
                    DomainExperimentData.get(domainClass,RunType.TEST).fromInstance,
                    DomainExperimentData.get(domainClass,RunType.TEST).toInstance,
                    domainParams);
        }
    }
}
