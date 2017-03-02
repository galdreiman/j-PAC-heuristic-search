package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.collections.PackedElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Here we hook onto the SearchAwarePACCondition
 * class to collect a single state for every observed h-value.
 * The tricky part is to collect this in a uniform way (per h value).
 */
public class StatesCollector implements SearchAwarePACCondition{
    private Random randomGenerator;
    private double fmin=-1;
    private double incumbent=-1;

    // There are the outputs of the collection process: how many states in every h value
    public Map<Double,Integer> hToCount;
    // A state randomly chosen from all states with the same h value
    public Map<Double,PackedElement> hToRepresentativeState;

    public StatesCollector(){}

    @Override
    public void removedFromOpen(AnytimeSearchNode node) {}

    @Override
    /**
     * Count how many nodes are generated for each h value
     */
    public void addedToOpen(AnytimeSearchNode node) {
        if(this.hToCount.containsKey(node.h)) {
            int hCount = this.hToCount.get(node.h) + 1;
            this.hToCount.put(node.h, hCount);

            // We want to choose uniformly from all states with the same h value
            // so we replace with some probability the representative state.
            // To give a fair chance to the states in the beginning of the search
            // the probability to replace decrease with the h-count. (the math works out nice)
            // To account for having mor
            if(this.randomGenerator.nextDouble()<1/hCount)
                this.hToRepresentativeState.put(node.h,node.packed);
        }
        else {
            this.hToCount.put(node.h, 1);
            this.hToRepresentativeState.put(node.h, node.packed);
        }
    }

    @Override
    public void setFmin(double fmin) {
        this.fmin=fmin;
        if(this.incumbent!=-1)
            if(this.incumbent/this.fmin==1) // Only halt if optimal
                throw new PACConditionSatisfied(this);
    }

    @Override
    public void setIncumbent(double incumbent, List<AnytimeSearchNode> openNodes) {
        this.incumbent=incumbent;
        if(this.fmin!=-1)
            if(this.incumbent/this.fmin==1) // Only halt if optimal
                throw new PACConditionSatisfied(this);
    }

    @Override
    public boolean shouldStop(SearchResult incumbentSolution) {
        return false;
    }

    @Override
    public void setup(SearchDomain domain, double epsilon, double delta) {
        this.hToCount = new HashMap();
        this.hToRepresentativeState = new HashMap();
        this.randomGenerator = new Random();
    }
}