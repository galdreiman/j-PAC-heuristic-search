package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.pac.conditions.AbstractPACCondition;

/**
 * Created by Roni Stern on 24/02/2017.
 *
 * This is a trivial PAC condition, that only halts after verifying with certainty
 * that the incumbent solution is at most 1+epsilon times the optimal solution.
 * Note, there is no consideration of delta in this condition.
 */
public class FMinCondition extends AbstractPACCondition {
    @Override
    public boolean shouldStop(SearchResult incumbentSolution) {
        if(incumbentSolution.hasSolution()==false)
            return false;

        double fmin = (Double)incumbentSolution.getExtras().get("fmin");
        double incumbent = incumbentSolution.getBestSolution().getCost();

        if (incumbent/fmin <= 1+epsilon)
            return true;
        else
            return false;
    }
}
