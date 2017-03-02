package org.cs4j.core.test.algorithms.pac;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.domains.*;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.test.algorithms.TestUtils;
import org.junit.Test;

import java.io.FileNotFoundException;
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
        testDeltaEffect(TrivialPACCondition.class);
    }
    @Test
    public void testFMinSetup()    {
        testSetup(new FMinCondition());
        testDeltaEffect(FMinCondition.class);
    }
    @Test
    public void testThresholdTooLow(){
        int instanceId = 52;
        Double delta = 0.25;
        Double epsilon = 0.1;

        PACUtils.loadPACStatistics(DockyardRobot.class);
        TrivialPACCondition pacCondition = new TrivialPACCondition();
        SearchDomain instance = ExperimentUtils.getSearchDomain(DockyardRobot.class,
                instanceId); // Arbitrary instance
        pacCondition.setup(instance,epsilon,delta);

        SearchResult result = new SearchResultsStub(50);
        result.getExtras().put("fmin",1.0);

        Assert.assertTrue("Should have halted",pacCondition.shouldStop(result));
    }


    @Test
    public void testFMinDelta() throws FileNotFoundException {
        SearchDomain domain = TestUtils.createDockyardRobot("52");

        PACSearchFramework psf = new PACSearchFramework();
        psf.setAnytimeSearchClass(AnytimePTS4PAC.class);
        psf.setPACConditionClass(FMinCondition.class);

        // Zero delta
        psf.setAdditionalParameter("epsilon","0.0");
        psf.setAdditionalParameter("delta","0.0");
        SearchResult result = psf.search(domain);
        long expanded0 = result.getExpanded();

        psf.setPACConditionClass(FMinCondition.class);
        psf.setAdditionalParameter("delta","1.0");
        result = psf.search(domain);
        long expanded1 = result.getExpanded();
        Assert.assertEquals(expanded0,expanded1);
        logger.info(expanded0);
        logger.info(expanded1);
    }


    public void testDeltaEffect(Class<? extends PACCondition> conditionClass){
        Class[] domains = {DockyardRobot.class,Pancakes.class,VacuumRobot.class,GridPathFinding.class};
        SearchDomain instance;
        PACSearchFramework psf;
        SearchResult result;
        int instanceId = 51;
        double oldExpanded;
        double newExpanded;
        for(Class domainClass :domains){
            logger.info("Testing domain " + domainClass.getName());
            for(Double epsilon : new Double[]{0.0,0.1, 0.25, 0.5, 0.75, 1.0}){
                oldExpanded = Double.MAX_VALUE;
                for(Double delta : new Double[]{0.0,0.1, 0.25, 0.5, 0.75, 1.0}){
                    logger.info("Testing eps="+epsilon+",delta="+delta);
                    PACUtils.loadPACStatistics(domainClass);
                    psf = new PACSearchFramework();
                    psf.setAnytimeSearchClass(AnytimePTS4PAC.class);
                    psf.setPACConditionClass(conditionClass);
                    psf.setAdditionalParameter("delta",""+delta);
                    psf.setAdditionalParameter("epsilon",""+epsilon);

                    instance = ExperimentUtils.getSearchDomain(domainClass, instanceId); // Arbitrary instance
                    result = psf.search(instance);

                    Assert.assertTrue(result.hasSolution());
                    newExpanded = result.getExpanded();
                    Assert.assertTrue("oldExpanded="+oldExpanded+", newExpanded="+newExpanded,
                            oldExpanded>=newExpanded);

                    // Fmin unaffected by delta
                    if(conditionClass.equals(FMinCondition.class) && oldExpanded<Double.MAX_VALUE){
                        Assert.assertEquals(newExpanded,oldExpanded);
                    }
                    oldExpanded = result.getExpanded();
                }

            }
        }
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
