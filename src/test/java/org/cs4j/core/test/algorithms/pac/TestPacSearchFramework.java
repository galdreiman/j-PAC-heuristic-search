package org.cs4j.core.test.algorithms.pac;

import junit.framework.Assert;
import org.cs4j.core.AnytimeSearchAlgorithm;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.algorithms.pac.AnytimePTS4PAC;
import org.cs4j.core.algorithms.pac.FMinCondition;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.domains.GridPathFinding;
import org.cs4j.core.test.algorithms.TestUtils;
import org.junit.Test;

import java.io.FileNotFoundException;

/**
 * Created by user on 24/02/2017.
 */
public class TestPacSearchFramework {

    /**
     * Verify that the given anytime search algorithm improves the solution quality every time it is run
     * and that it ends only when it has the optimal solution cost.
     */
    public void testFMinPacCondition(SearchAlgorithm algorithm,double epsilon) throws FileNotFoundException {
        SearchDomain[] domains = {
                TestUtils.createFifteenPuzzle("12"),
                TestUtils.createPancakePuzzle(12,"12"),
                TestUtils.createGridPathFinding("brc202d.map","12"),
                TestUtils.createVacuumRobot(5,"12"),
                TestUtils.createDockyardRobot("12")
        };
        for(SearchDomain domain : domains){
            SearchResult pacResults = algorithm.search(domain);
            Assert.assertTrue(pacResults.hasSolution());
            SearchResult optimalResults = TestUtils.findOptimalSolution(domain);

            // Check that suboptimality bound remains at most 1+epsilon
            double solutionCost = pacResults.getSolutions().get(0).getCost();
            double optimalCost = optimalResults.getSolutions().get(0).getCost();
            Assert.assertTrue(
                    "The ratio between " +solutionCost+" and optimal " +optimalCost + " is not sufficient",
                    solutionCost/optimalCost<=(1+epsilon));
       }
    }

    /**
     * Test the Fmin condition for a range of epsilon
     * (not that since in this condition delta doesn't matter, we do not vary its values)
     * @throws FileNotFoundException
     */
    @Test
    public void testPACAnytimePTS() throws FileNotFoundException
    {
        double[] epsilons = {0, 0.1, 0.5, 1, 2};
        SearchAlgorithm pacSearch = new PACSearchFramework();
        pacSearch.setAdditionalParameter("delta","1");
        pacSearch.setAdditionalParameter("anytimeSearch",AnytimePTS.class.getName());
        pacSearch.setAdditionalParameter("pacCondition",FMinCondition.class.getName());
        for(double epsilon : epsilons)
        {
            pacSearch.setAdditionalParameter("epsilon",""+epsilon);
            testFMinPacCondition(pacSearch,epsilon);
        }
    }

    @Test
    public void testPACHalts() throws FileNotFoundException{
        SearchAlgorithm pacSearch = new PACSearchFramework();
        pacSearch.setAdditionalParameter("delta","1");
        pacSearch.setAdditionalParameter("anytimeSearch",AnytimePTS.class.getName());
        pacSearch.setAdditionalParameter("pacCondition",FMinCondition.class.getName());
        pacSearch.setAdditionalParameter("epsilon",""+0);

        SearchDomain domain = TestUtils.createFifteenPuzzle("98");
        SearchResult pacResults = pacSearch.search(domain);
        Assert.assertTrue(pacResults.hasSolution());

        SearchResult optimalResults = TestUtils.findOptimalSolution(domain);
        double solutionCost = pacResults.getSolutions().get(0).getCost();
        double optimalCost = optimalResults.getSolutions().get(0).getCost();
        Assert.assertTrue("A suboptimal solution was found",
                    solutionCost>optimalCost);
    }


    @Test
    public void testConvergeToOptimal() throws FileNotFoundException {
        SearchDomain domain = TestUtils.createGridPathFinding("brc202d.map","1");
        //AnytimeSearchAlgorithm apts = new AnytimePTS();
        AnytimePTS4PAC apts = new AnytimePTS4PAC();
        apts.setPacCondition(new FMinCondition());
        SearchResult result = apts.search(domain);
        int iteration=0;
        while(result.hasSolution()){
            iteration++;
            System.out.println(iteration+","+result.getBestSolution().getCost());
            Assert.assertEquals(iteration, apts.getTotalSearchResults().getSolutions().size());
            result = apts.continueSearch();
        }
        result = apts.getTotalSearchResults();
        Assert.assertTrue(result.hasSolution());
        double observedOptimal = result.getBestSolution().getCost();
        double expectedOptimal = PACUtils.getOptimalSolution(GridPathFinding.class,1);

        Assert.assertEquals(expectedOptimal,observedOptimal);
    }

    @Test
    public void testPACOptimal() throws FileNotFoundException {
        SearchDomain domain = TestUtils.createGridPathFinding("brc202d.map","1");
        //AnytimeSearchAlgorithm apts = new AnytimePTS();
        double optimal = PACUtils.getOptimalSolution(GridPathFinding.class,1);

        // Run PSF with epsilon and delta zero, and verify that the result in the optimal solution
        PACSearchFramework psf = new PACSearchFramework();
        psf.setAdditionalParameter("epsilon", "" + 0);
        psf.setAdditionalParameter("delta", "" + 0);
        psf.setAdditionalParameter("pacCondition", FMinCondition.class.getName());
        psf.setAdditionalParameter("anytimeSearch",AnytimePTS4PAC.class.getName());

        SearchResult result = psf.search(domain);

        Assert.assertTrue(result.hasSolution());
        Assert.assertEquals(result.getBestSolution().getCost(),optimal);
        Assert.assertEquals(result.getSolutions().size(),9);
    }

    @Test
    public void testPACSubptimal() throws FileNotFoundException {
        SearchDomain domain = TestUtils.createGridPathFinding("brc202d.map","1");
        //AnytimeSearchAlgorithm apts = new AnytimePTS();
        double optimal = PACUtils.getOptimalSolution(GridPathFinding.class,1);

        // Run PSF with epsilon and delta zero, and verify that the result in the optimal solution
        PACSearchFramework psf = new PACSearchFramework();
        psf.setAdditionalParameter("epsilon", "" + 10000);
        psf.setAdditionalParameter("delta", "" + 0);
        psf.setAdditionalParameter("pacCondition", FMinCondition.class.getName());
        psf.setAdditionalParameter("anytimeSearch",AnytimePTS4PAC.class.getName());

        SearchResult result = psf.search(domain);

        Assert.assertTrue(result.hasSolution());
        Assert.assertTrue(result.getBestSolution().getCost()>optimal);
        Assert.assertEquals(result.getSolutions().size(),1);
    }
}
