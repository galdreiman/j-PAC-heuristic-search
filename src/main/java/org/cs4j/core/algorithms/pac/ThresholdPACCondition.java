package org.cs4j.core.algorithms.pac;

import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Roni Stern on 26/02/2017.
 *
 * This is a super class for all the PAC conditions that are based on a static threshold
 * that does not change during the search. That is, these are the non-search-aware conditions
 * - Trivial and RatioBased.
 */
public abstract class ThresholdPACCondition extends AbstractPACCondition {

    final static Logger logger = Logger.getLogger(ThresholdPACCondition.class);


    // If a solution is equal to or lower than this bound - we can halt
    protected double threshold;


    @Override
    public void setup(SearchDomain domain, double epsilon, double delta) {
        super.setup(domain,epsilon,delta);

        // Load statistics
        PACStatistics statistics = PACUtils.getStatisticsFile(this, domain.getClass());

        // 1. Process the statistics
        // Building the PDF (  cost -> prob. that optimal is less than or equal to cost)
        SortedMap<Double, Double> costToCDF = computeCDF(statistics);

        // Dump statistics (for DEBUG)
        dumpCDFToFile(DomainExperimentData.get(domain.getClass(),RunType.TRAIN).outputPath
                +this.getClass().getSimpleName()+"-statistics.csv", costToCDF);

        // Compute threshold
        threshold = this.computeThreshold(epsilon,delta,costToCDF);
    }

    @Override
    public boolean shouldStop(SearchResult incumbentSolution) {
        // Check if fmin is high enough to stop
        if(incumbentSolution.hasSolution()==false)
            return false;

        double fmin = (Double)incumbentSolution.getExtras().get("fmin");
        double incumbent = incumbentSolution.getBestSolution().getCost();

        if (incumbent/fmin <= 1+epsilon)
            return true;

        // If fmin is not high enough, consider the PAC threshold
        return incumbentSolution.getBestSolution().getCost()<=this.threshold;
    }

    /**
     * Computes a cumulative distribution function (CDF) based on the given PAC statistics.
     *
     * @param statistics a map of instance to relevant statistics
     * @return a map of solution cost value to the corresponding CDF.
     */
    protected abstract SortedMap<Double, Double> computeCDF(PACStatistics statistics);

    /**
     * Dump statitics to file
     * @param outputFileName the output file name
     */
    private void dumpCDFToFile(String outputFileName,SortedMap<Double, Double> costToCDF) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
            writer.write("Cost, Pr(OPT<Cost)");
            writer.newLine();
            for (Double cost : costToCDF.keySet()) {
                writer.write(cost + "," + costToCDF.get(cost));
                writer.newLine();
            }
            writer.close();
        }catch(IOException exception){
            logger.error("Statistics.dumpToFile failed",exception);
        }
    }

    /**
     * Compute the cost threshold that would satisfy finding a PAC condition
     * @param epsilon desired suboptimality bound
     * @param delta required confidence
     * @return the maximal cost that ensured these PAC parameters
     */
    public double computeThreshold(double epsilon, double delta,SortedMap<Double, Double> costToCDF){
        Double oldCdfValue = 0.0;
        double cdfValue;
        for(Double cost : costToCDF.keySet()) { // Note that costsToCDF is a sorted list!
            cdfValue=costToCDF.get(cost);
            if(cdfValue==delta)
                return cost*(1+epsilon);
            if (cdfValue>delta)
                return oldCdfValue*(1+epsilon);
            oldCdfValue=cdfValue;
        }
        throw new IllegalStateException("CDF must sum up to one, so delta value must be met (delta was "+delta+" and last CDF value was "+oldCdfValue+")");
    }
}
