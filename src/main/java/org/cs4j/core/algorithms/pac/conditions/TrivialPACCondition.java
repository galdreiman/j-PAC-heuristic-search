package org.cs4j.core.algorithms.pac;

import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.*;
import java.util.*;



/**
 * Created by user on 26/02/2017.
 *
 * The Trivial PAC condition.
 */
public class TrivialPACCondition extends ThresholdPACCondition {

    final static Logger logger = Logger.getLogger(TrivialPACCondition.class);



    /**
     * Computes a cumulative distribution function (CDF) based on the given PAC statistics.
     *
     * @param statistics a map of instance to relevant statistics
     * @return a map of solution cost value to the corresponding CDF.
     */
    @Override
    protected SortedMap<Double, Double> computeCDF(PACStatistics statistics){
        SortedMap<Double, Double> costToPDF = new TreeMap<>();
        Collection<Double> optimalSolutions = statistics.instanceToOptimal.values();
        for(Double optimalCost : optimalSolutions){
            if(costToPDF.containsKey(optimalCost)==false)
                costToPDF.put(optimalCost,1.0);
            else
                costToPDF.put(optimalCost,(costToPDF.get(optimalCost)+1));
        }
        for(Double cost : costToPDF.keySet())
            costToPDF.put(cost,(costToPDF.get(cost)/optimalSolutions.size()));

        // Building the CDF (cumulative)
        SortedMap<Double, Double> costToCDF = new TreeMap<>();
        Double oldCDFValue=0.0;
        for(Double cost : costToPDF.keySet()) {
            costToCDF.put(cost, costToPDF.get(cost) + oldCDFValue);
            oldCDFValue = costToCDF.get(cost);
        }

        // Accuracy issues
        if(oldCDFValue!=1.0){
            assert Math.abs(oldCDFValue-1.0)<0.0001; // Verifying this is just an accuracy issue
            costToCDF.put(costToCDF.lastKey(),1.0);
        }
        return costToCDF;
    }
}
