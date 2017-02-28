package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by user on 28/02/2017.
 */
public class OpenBasedPACCondition extends RatioBasedPACCondition implements SearchAwarePACCondition{
    // This is the probability that the incumbent solution does not have the desired suboptimality
    // We maintain this value and change it during the search, halting when it is lower than delta.
    private double probNotSuboptimal;
    private double fmin;
    private double incumbent;

    // This shows the condition that was fired, if a PACConditionSatisifed has been thrown
    // This knowledge is mainly used for analyzing the experimental results
    public Condition conditionFired=null;
    public enum Condition {
        FMIN, OPTIMAL, OPEN_BASED
    }

    // Maps to a given h value the CDF for it. Every h value represents the range of h values up to it
    // sinde the last h value
    private SortedMap<Double, SortedMap<Double,Double>> hToCdf;

    @Override
    public boolean shouldStop(SearchResult incumbentSolution) {
        return this.probNotSuboptimal<=this.delta;
    }

    @Override
    public void setup(SearchDomain domain, double epsilon, double delta) {
        super.setup(domain,epsilon,delta);
        this.probNotSuboptimal=1;
        this.hToCdf = this.createCDFs();
    }


    /**
     * Builds a CDF of the h-ratios, but group by according to the h ranges
     * Assumed that the given h values and hratios are correlated
     * and sorted by ascending order
     *
     * @param hRanges sets the h bins
     * @param hToOptimalTuples the h values and their corresponding optimal values
     * @return hToCdf - maps an value to a CDF of optimal to h ratios
     */
    public SortedMap<Double, SortedMap<Double,Double>> createCDFs(List<Double> hRanges,
                                                                  List<Tuple<Double,Double>> hToOptimalTuples){
        int i=0;int j=0;
        List<Double> ratios= new ArrayList<>();
        SortedMap<Double, SortedMap<Double,Double>> hToCDF = new TreeMap<>();
        Tuple<Double,Double> hToOptimal = hToOptimalTuples.get(i);
        Double hRange = hRanges.get(j);

        do{
            // If range >= h, add opt/h to the statistics
            if(hRange>=hToOptimal._1) {
                ratios.add(hToOptimal._2 / hToOptimal._1);
                if(i<hToOptimalTuples.size()-1){
                    i++;
                    hToOptimal = hToOptimalTuples.get(i);
                }
                else break; // Finished all the data
            }
            else{ // range < h, increment range
                j++;
                hRange = hRanges.get(j);
                assert j<hRanges.size(); // h ranges must end with Double.MAX_VALUE
                hToCDF.put(hRange,PACUtils.computeCDF(ratios));
                ratios.clear();
            }
        }while(i<hToOptimalTuples.size()-1);
        return hToCDF;
    }


    /**
     * Builds a CDF of the h-ratios, but group by according to the h ranges
     * Assumed that the given h values and hratios are correlated
     * and sorted by ascending order
     *
     * @return hToCdf - maps an value to a CDF of optimal to h ratios
     */
    public SortedMap<Double, SortedMap<Double,Double>>  createCDFs(){
        List<Tuple<Double,Double>> hToOptimalTuples = PACUtils.getHtoOptimalTuples(this.domain.getClass());
        List<Double> hRanges = this.computeHRanges(hToOptimalTuples);
        return this.createCDFs(hRanges, hToOptimalTuples);
    }

    /**
     * Compute the bins of h values according to which to compute CDFs for the OpenBasedPACondition
     */
    private List<Double> computeHRanges(List<Tuple<Double, Double>> hToOptimalTuples) {
        // First pass: hRanges according by grouping at least 50 tuples together
        List<Tuple<Double,Double>> tuples = new ArrayList<>();
        SortedMap<Double, List<Tuple<Double,Double>>> hToTuples = new TreeMap<>();
        int counter = 0;
        Double oldH=0.0;
        for(Tuple<Double,Double> hToOpt : hToOptimalTuples){
            // Partition according to h values, but verify that at least 50 instances per partition
            if(counter>=50){
                if(hToOpt._1!=oldH){
                    //logger.debug("Counter="+counter+", h="+oldH);
                    hToTuples.put(oldH,new ArrayList<>(tuples));
                    counter=0;
                    tuples.clear();;
                }
            }
            tuples.add(hToOpt);
            oldH = hToOpt._1;
            counter++;
        }
        // Put the leftovers with the last partition, under the Double.maxvalue partition
        Double lastHRange = hToTuples.lastKey();
        tuples.addAll(hToTuples.get(lastHRange));
        hToTuples.remove(lastHRange);
        hToTuples.put(Double.MAX_VALUE,tuples);


        // Second pasS: join hranges with the same average ratio
        double averageRatio;
        List<Double> hRanges = new ArrayList<>();
        oldH = 0.0;
        double oldAverage = 0.0;
        for(Double h : hToTuples.keySet()){
            tuples = hToTuples.get(h);
            averageRatio = computeAverageRatio(tuples);
            if(oldH>0){
                // If the previous h range had almost the same average - join them
                // @TODO: Using the average is a heuristic for having a similar distribution
                // @TODO: Future work may be to considder distribution distances, e.g., KL divergence
                if(Math.abs(oldAverage-averageRatio)<=0.1)
                    hRanges.remove(oldH);
                else
                    oldAverage=averageRatio;

                hRanges.add(h);
            }
            oldH = h;
        }

        // DEBUG
        for(Double h:hRanges)
            logger.info("h rangle = "+ h);

        return hRanges;
    }

    /**
     * Computes the average ratio between the second and first elements in the tuples
     */
    private double computeAverageRatio(List<Tuple<Double,Double>> tuples){
        double sum=0;
        for(Tuple<Double,Double> tuple : tuples){
            sum+= tuple._2/tuple._1;
        }
        return sum/tuples.size();
    }

    /**
     * Computes a cumulative distribution function (CDF) of the heuristic error,
     * that is, the ratio between opt and the initial h (h(start))
     *
     * @param statistics a map of instance to relevant statistics
     * @return a map of solution cost value to the corresponding CDF.
     */
    @Override
    protected SortedMap<Double, Double> computeCDF(PACStatistics statistics){
        Map<Integer,Double> instanceToOptimal = PACUtils.getOptimalSolutions(domain.getClass());
        Map<Integer,Double> instanceToInitialH = PACUtils.getInitialHValues(domain.getClass());


        // Building the PDF (  cost -> prob. that optimal is less than or equal to cost)
        SortedMap<Double, Double> ratioToPDF = new TreeMap<Double, Double>();
        double ratio;
        for(Integer instance : instanceToOptimal.keySet()){
            ratio = instanceToOptimal.get(instance)/instanceToInitialH.get(instance);
            if(ratioToPDF.containsKey(ratio)==false)
                ratioToPDF.put(ratio,1.0);
            else
                ratioToPDF.put(ratio,(ratioToPDF.get(ratio)+1));
        }
        for(Double cost : ratioToPDF.keySet())
            ratioToPDF.put(cost,(ratioToPDF.get(cost)/instanceToOptimal.size()));

        // Building the CDF (cumulative)
        SortedMap<Double, Double>ratioToCDF = new TreeMap<Double, Double>();
        Double oldCDFValue=0.0;
        for(Double cost : ratioToPDF.keySet()) {
            ratioToCDF.put(cost*this.initialH, ratioToPDF.get(cost) + oldCDFValue);
            oldCDFValue = ratioToCDF.get(cost*this.initialH);
        }

        // Accuracy issues
        if(oldCDFValue!=1.0){
            assert Math.abs(oldCDFValue-1.0)<0.0001; // Verifying this is just an accuracy issue
            ratioToCDF.put(ratioToCDF.lastKey(),1.0);
        }

        return ratioToCDF;
    }


    /**
     * This returns the probability that the incumbent solution is smaller
     * than 1+epsilon times the cost of the optimal path from the initial state
     * to the goal state that passes through this node.
     * ion an
     * @param node The node
     * @return The prob. that it cannto invalidate the PAC-ness of the incumbent
     */
    private double getProb(AnytimeSearchNode node){
        // Maps an h*-to-h ratio to likelihood that the actual h-to-h* is smaller or equal
        SortedMap<Double, Double> cdf = getCDF(node);
        Double oldCdfValue = 0.0;

        // If the h*-to-h ratio is smaller than this value, our solution is not PAC
        double sufficientRatio = ((this.incumbent/(1+this.epsilon))-node.g)/node.h;
        for(Double ratio: cdf.keySet()) { // Note that costsToCDF is a sorted list!
            if(ratio>=sufficientRatio)
                return 1-cdf.get(ratio); // @TODO: This is a conservative estimate

        }
        throw new IllegalStateException("CDF must sum up to one, so delta value must be met (delta was "+delta+" and last CDF value was "+oldCdfValue+")");
    }

    /**
     * Finds the suitable CDF for the given node
     * @param node the node to find a CDF for
     * @return a CDF
     */
    private SortedMap<Double, Double> getCDF(AnytimeSearchNode node){
        for(Double hValue : this.hToCdf.keySet()){
            if(node.h>=hValue)
                return this.hToCdf.get(hValue);

        }
        return this.hToCdf.get(this.hToCdf.lastKey());
    }


    public void removedFromOpen(AnytimeSearchNode node)
    {
        this.probNotSuboptimal=this.probNotSuboptimal/this.getProb(node);
    }
    public void addedToOpen(AnytimeSearchNode node)
    {
        this.probNotSuboptimal=this.probNotSuboptimal*this.getProb(node);
    }

    /**
     * Fmin has been updated. Check if a PAC conddition is satisfied
     * @param fmin
     */
    public void setFmin(double fmin){
        this.fmin=fmin;
        if(this.incumbent/this.fmin < (1+this.epsilon)) {
            this.conditionFired=Condition.FMIN;
            throw new PACConditionSatisfied(this);
        }
    }

    /**
     * A new incumbent solution has been found.
     * @param incumbent The cost of the new incumbent solutions
     * @param openNodes The nodes in the open list
     */
    public void setIncumbent(double incumbent, List<AnytimeSearchNode> openNodes){
        this.incumbent=incumbent;

        // Check fmin
        if(this.incumbent/this.fmin < (1+this.epsilon)) {
            this.conditionFired=Condition.FMIN;
            throw new PACConditionSatisfied(this);
        }

        // Recompute the prob not suboptimal
        this.probNotSuboptimal=1;
        for(AnytimeSearchNode node : openNodes) {
            this.probNotSuboptimal=this.probNotSuboptimal*
                    this.getProb(node);
        }
        if(this.probNotSuboptimal<=this.delta) {
            this.conditionFired=Condition.OPEN_BASED;
            throw new PACConditionSatisfied(this);
        }
    }
}
