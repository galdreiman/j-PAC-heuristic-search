package org.cs4j.core.test.algorithms.pac;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.domains.DockyardRobot;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.experiments.PacPreprocessRunner;
import org.junit.Test;

/**
 * Created by Roni Stern on 01/03/2017.
 */
public class TestOraclePACCondition {
    final static Logger logger = Logger.getLogger(TestOraclePACCondition.class);

    @Test
    public void testOraclePACCondition() {
        int instanceId=91;
        SearchDomain instance = ExperimentUtils.getSearchDomain(DockyardRobot.class, instanceId);
        PACSearchFramework psf = new PACSearchFramework();
        OraclePACCondition pacCondition = new OraclePACCondition();

        psf.setAnytimeSearchAlgorithm(new AnytimePTS4PAC());
        psf.setPACCondition(pacCondition);
        psf.setAdditionalParameter("epsilon", "1");
        psf.setAdditionalParameter("delta", "0");

        Double optimalSolution = PACUtils.getOptimalSolutions(instance.getClass()).get(instanceId);
        pacCondition.setOptimalSolution(optimalSolution);

        SearchResult result = psf.search(instance);
        Assert.assertTrue(result.hasSolution());
    }

    @Test
    public void testPanacke54e01() {
        int instanceId=54;
        Double epsilon = 0.1;
        SearchDomain instance = ExperimentUtils.getSearchDomain(Pancakes.class, instanceId);
        PACSearchFramework psf = new PACSearchFramework();
        OraclePACCondition pacCondition = new OraclePACCondition();

        psf.setAnytimeSearchAlgorithm(new AnytimePTS4PAC());
        psf.setPACCondition(pacCondition);
        psf.setAdditionalParameter("epsilon", ""+0.1);
        psf.setAdditionalParameter("delta", "0");

        logger.info("Run Oracle");
        Double optimalSolution = PACUtils.getOptimalSolutions(instance.getClass()).get(instanceId);
        pacCondition.setOptimalSolution(optimalSolution);
        SearchResult result = psf.search(instance);
        Assert.assertTrue(result.hasSolution());
        double incumbent = result.getBestSolution().getCost();
        Assert.assertTrue((1+epsilon)*optimalSolution>=incumbent);
        double oracleExpanded = result.getExpanded();


        logger.info("Run Fmin");
        FMinCondition fminCondition = new FMinCondition();
        psf.setPACCondition(fminCondition);
        result = psf.search(instance);
        Assert.assertTrue(result.hasSolution());
        incumbent = result.getBestSolution().getCost();
        Assert.assertTrue("Incumbent="+incumbent+", optimal="+optimalSolution,(1+epsilon)*optimalSolution>=incumbent);

        double fminExpanded = result.getExpanded();

        Assert.assertTrue("Oracle="+oracleExpanded+", FMin="+fminExpanded, oracleExpanded<=fminExpanded);
    }

    @Test
    public void testOraclePACConditionInPancakes() {
        SearchDomain instance;
        PACSearchFramework psf = new PACSearchFramework();
        OraclePACCondition pacCondition = new OraclePACCondition();
        double epsilon=1;
        int[] instances = new int[]{51,52,53};
        SearchResult result=null;
        Double optimalSolution;

        int instanceId;

        for(epsilon=3; epsilon>=0;epsilon = epsilon-0.25) {
            logger.info("Running on esp="+epsilon);
            for(int j=0;j<instances.length;j++){
                instanceId = instances[j];
                instance = ExperimentUtils.getSearchDomain(Pancakes.class, instanceId);
                optimalSolution = PACUtils.getOptimalSolutions(instance.getClass()).get(instanceId);
                pacCondition.setOptimalSolution(optimalSolution);
                psf.setAnytimeSearchAlgorithm(new AnytimePTS4PAC());
                psf.setPACCondition(pacCondition);
                psf.setAdditionalParameter("epsilon", "" + epsilon);
                psf.setAdditionalParameter("delta", "0");

                result = psf.search(instance);
                Assert.assertTrue(result.hasSolution());
                Assert.assertTrue(result.getBestSolution().getCost() / optimalSolution <= 1 + epsilon);

                if(epsilon==0)
                    Assert.assertEquals(result.getBestSolution().getCost(),optimalSolution);
            }
        }
    }

    @Test
    public void testOracleDockyard53Optimal() {
        int instanceId=53;
        SearchDomain instance = ExperimentUtils.getSearchDomain(DockyardRobot.class, instanceId);
        PACSearchFramework psf = new PACSearchFramework();
        OraclePACCondition pacCondition = new OraclePACCondition();

        psf.setAnytimeSearchAlgorithm(new AnytimePTS4PAC());
        psf.setPACCondition(pacCondition);
        psf.setAdditionalParameter("epsilon", "0.0");
        psf.setAdditionalParameter("delta", "0.0");

        Double optimalSolution = PACUtils.getOptimalSolutions(instance.getClass()).get(instanceId);
        pacCondition.setOptimalSolution(optimalSolution);
        SearchResult result = psf.search(instance);

        Assert.assertTrue(result.hasSolution());
        Assert.assertEquals(result.getBestSolution().getCost(),optimalSolution);
    }

    @Test
    public void testOptimal(){
        int instanceId=54;
        SearchDomain instance = ExperimentUtils.getSearchDomain(DockyardRobot.class, 1);

        WAStar alg = new WAStar();
        alg.setAdditionalParameter("weight","1.0");
        SearchResult result = alg.search(instance);

        Assert.assertEquals(23,result.getBestSolution().getCost());
    }



    @Test
    public void testOracleDockyard() {
        int instanceId=53;
        SearchDomain instance = ExperimentUtils.getSearchDomain(DockyardRobot.class, instanceId);
        PACSearchFramework psf = new PACSearchFramework();
        OraclePACCondition pacCondition = new OraclePACCondition();

        psf.setAnytimeSearchAlgorithm(new AnytimePTS4PAC());
        psf.setPACCondition(pacCondition);
        psf.setAdditionalParameter("epsilon", "1.0");
        psf.setAdditionalParameter("delta", "0.0");

        Double optimalSolution = PACUtils.getOptimalSolutions(instance.getClass()).get(instanceId);
        pacCondition.setOptimalSolution(optimalSolution);

        SearchResult result = psf.search(instance);

        Assert.assertTrue(result.hasSolution());
        double expanded10 = result.getExpanded();


        logger.info("Search with eps=0.25");
        psf = new PACSearchFramework();
        pacCondition = new OraclePACCondition();
        pacCondition.setOptimalSolution(optimalSolution);

        psf.setAnytimeSearchAlgorithm(new AnytimePTS4PAC());
        psf.setPACCondition(pacCondition);
        psf.setAdditionalParameter("epsilon", "0.25");
        psf.setAdditionalParameter("delta", "0.0");

        result = psf.search(instance);
        Assert.assertTrue(result.hasSolution());
        double expanded025= result.getExpanded();

        Assert.assertTrue("Expanded " + expanded10 +" for e=1.0 and " + expanded025 + " for e=0.25",
                expanded10<expanded025);
    }

}
