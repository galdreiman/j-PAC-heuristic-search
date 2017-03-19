package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Roni Stern on 01/03/2017.
 * This PAC condition represents an offline-optimal PAC condition.
 * We cannot hope to do any better.
 */
public class OraclePACCondition extends AbstractPACCondition{

    private Double optimalSolution;
    public void setOptimalSolution(Double optimalSolution){
        this.optimalSolution=optimalSolution;
    }

    @Override
    public boolean shouldStop(SearchResult incumbentSolution) {
        if(incumbentSolution.hasSolution()==false)
            return false;
        if(this.optimalSolution*(1+this.epsilon)>=incumbentSolution.getBestSolution().getCost())
            return true;
        else
            return false;
    }
}
