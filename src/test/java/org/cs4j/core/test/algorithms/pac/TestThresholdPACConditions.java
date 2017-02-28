package org.cs4j.core.test.algorithms.pac;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.algorithms.pac.PACCondition;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.algorithms.pac.RatioBasedPACCondition;
import org.cs4j.core.algorithms.pac.TrivialPACCondition;
import org.cs4j.core.domains.*;
import org.cs4j.core.experiments.ExperimentUtils;
import org.junit.Test;

import java.util.List;

/**
 * Created by user on 26/02/2017.
 *
 * Test case for the threshold-based  PAC condition
 */
public class TestThresholdPACConditions {

    final static Logger logger = Logger.getLogger(TestThresholdPACConditions.class);
    @Test
    public void testRatioBased(){
        testSetup(new RatioBasedPACCondition());
    }

    @Test
    public void testTrivialSetup()    {
        testSetup(new TrivialPACCondition());
    }


    /**
     * Tests the setup function of a given condition and some extreme values
     * @param condition the conditon to evaluate
     */
    private void testSetup(PACCondition condition){

        Class[] domains = {
                FifteenPuzzle.class,
                Pancakes.class,
                VacuumRobot.class,
                DockyardRobot.class,
                GridPathFinding.class};

        for(Class domainClass :domains){
            logger.info("Testing domain "+domainClass.getName());
            PACUtils.loadPACStatistics(domainClass);
            SearchDomain instance = ExperimentUtils.getSearchDomain(domainClass,12);
            SearchResult resultZero = new SearchResultsStub(0);
            SearchResult resultMax = new SearchResultsStub(Double.MAX_VALUE);

            // We don't want the f-min rule to be used here
            resultMax.getExtras().put("fmin",1.0);
            resultZero.getExtras().put("fmin",1.0);

            condition.setup(instance,0,0);
            Assert.assertFalse(condition.shouldStop(resultMax));
            Assert.assertTrue("Solution of cost zero must be PAC", condition.shouldStop(resultZero));

            condition.setup(instance,1,0);
            Assert.assertFalse(condition.shouldStop(resultMax));
            Assert.assertTrue(condition.shouldStop(resultZero));

            condition.setup(instance,0,1);
            Assert.assertFalse(condition.shouldStop(resultMax));
            Assert.assertTrue(condition.shouldStop(resultZero));

            condition.setup(instance,1,1);
            Assert.assertFalse(condition.shouldStop(resultMax));
            Assert.assertTrue(condition.shouldStop(resultZero));
        }
    }


    // ----------------- STUBS ------------------------
    private class SolutionStub implements SearchResult.Solution{

        private double cost;

        public SolutionStub(double cost){this.cost=cost;}

        @Override
        public double getCost() {
            return this.cost;
        }

        @Override
        public List<SearchDomain.Operator> getOperators() {
            return null;
        }

        @Override
        public List<SearchDomain.State> getStates() {
            return null;
        }

        @Override
        public String dumpSolution() {
            return null;
        }


        @Override
        public int getLength() {
            return 0;
        }
    }



    private class SearchResultsStub extends SearchResultImpl
    {
        private Solution solution;

        public SearchResultsStub(double cost){
            this.addSolution(new SolutionStub(cost));
        }
    }

}
