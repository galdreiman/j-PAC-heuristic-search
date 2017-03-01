package org.cs4j.core.experiments;

import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;

import java.util.SortedMap;

/**
 * Created by Roni Stern on 01/03/2017.
 * Represents a single a experiment
 */
public interface Experiment {
    /**
     * Run an experiment on a single problem instance
     * @param instance the problem instance to run on
     * @param output where to output the results
     * @param instanceId the id of the problem instance (this is sometimes helpful)
     * @param runParams parameters of this run. This can be some configuration of the algorithm
     *                  you want to output in the results.
     */
    void run(SearchDomain instance,
                           OutputResult output,
                           int instanceId,
                           SortedMap<String, Object> runParams);
}
