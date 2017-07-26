package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.algorithms.pac.conditions.OpenBasedPACCondition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

/**
 * Created by Roni Stern on 03/03/2017.
 * Designed to output the condition for open based experiments.
 */
public class OpenBasedExperiment  extends StandardExperiment{
    final static Logger logger = Logger.getLogger(OpenBasedExperiment.class);

    public OpenBasedExperiment() {
        super(new PACSearchFramework());

        PACSearchFramework psf = (PACSearchFramework)this.searchAlgorithm;
        psf.setAnytimeSearchClass(SearchAwarePACSearchImpl.class);
        psf.setPACConditionClass(OpenBasedPACCondition.class);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
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

            resultsData = new ArrayList<>();
            resultsData.add(instanceId); // Instance ID
            appendSearchResults(result, resultsData); // Search results data
            // (expanded, cpu
            // time, etc.)
            resultsData.addAll(runParams.values()); // Parameters that
            // are constant for
            // this run (w,
            // domain, etc.)
            resultsData.add(instance.getClass().getSimpleName());

            // add OPT value
            PACStatistics stats = PACUtils.getPACStatistics(instance.getClass());
            double opt = stats.instanceToOptimal.get(instanceId);
            resultsData.add(opt);

            // add isEpsilon: isEpsilon= W*OPT >= Cost
            double cost = Double.parseDouble(resultsData.get(3).toString());
            double epsilon = Double.parseDouble(resultsData.get(10).toString());
            int isEpsilon = opt * (1+epsilon) >= cost == true? 1 : 0;
            resultsData.add(isEpsilon);

            output.appendNewResult(resultsData.toArray());
            output.newline();
        } catch (OutOfMemoryError e) {
            logger.info("OutOfMemory in:" + searchAlgorithm.getName() + " on:" + instance.getClass().getName());
        } catch (IOException e) {
            logger.info("IOException at instance " + instanceId);
        }
    }


    /**
     * Extract from SearchResults the data to output to the file
     * @param result the SearchResults object to extract from
     * @param resultsData a list to append the results extracted from SearchResults
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    protected void appendSearchResults(SearchResult result,
                                       List resultsData) {
        resultsData.add(result.hasSolution() ? 1 : 0);
        if (result.hasSolution()) {
            resultsData.add(result.getBestSolution().getLength());
            resultsData.add(result.getBestSolution().getCost());
        }else{
            resultsData.add(-1);
            resultsData.add(-1);
        }
        resultsData.add(result.getSolutions().size());
        resultsData.add(result.getGenerated());
        resultsData.add(result.getExpanded());
        resultsData.add(result.getCpuTimeMillis());
        resultsData.add(result.getWallTimeMillis());

        // Add condition fired
        if(result.getExtras().containsKey("PACSatisfied"))
            resultsData.add(result.getExtras().get("PACSatisfied"));
        else
            resultsData.add("Fmin?");
    }

    @Override
    public String[] getResultsHeaders() {
        return new String[] { "InstanceID", "Found", "Depth", "Cost", "Iterations", "Generated",
                "Expanded", "Cpu Time", "Wall Time","PACCondition" };
    }

}
