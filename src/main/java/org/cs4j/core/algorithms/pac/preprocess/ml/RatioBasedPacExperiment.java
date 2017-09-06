package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.pac.PACStatistics;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.experiments.MLPacExperiment;
import org.cs4j.core.experiments.StandardExperiment;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

/**
 * Created by Gal Dreiman on 06/09/2017.
 */
public class RatioBasedPacExperiment extends MLPacExperiment {
    final static Logger logger = Logger.getLogger(RatioBasedPacExperiment.class);

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

            int domainLevelTest = Integer.parseInt(runParams.get("domainLevelTest").toString());

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

            String statisticsFile = String.format(DomainExperimentData.get(instance.getClass(), DomainExperimentData.RunType.TRAIN).inputPathFormat, domainLevelTest)
                    + File.separator + PACStatistics.STATISTICS_FILE_NAME;
            PACStatistics statistics = PACUtils.parsePACStatisticsFile(statisticsFile);

//            PACStatistics stats = PACUtils.getPACStatistics(instance.getClass());
            double opt = statistics.instanceToOptimal.get(instanceId);
            resultsData.add(opt);

            // add isEpsilon: isEpsilon= W*OPT >= Cost
            double cost = Double.parseDouble(resultsData.get(3).toString());
            double epsilon = Double.parseDouble(runParams.get("epsilon").toString());
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
}
