package org.cs4j.core.test.algorithms;

import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.domains.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by user on 23/02/2017.
 */
public class TestUtils {
    public static SearchDomain createFifteenPuzzle(String instance) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("./input/fifteenpuzzle/korf100/"+instance+".in"));
        FifteenPuzzle puzzle = new FifteenPuzzle(is);
        return puzzle;
    }

    public static SearchDomain createPancakePuzzle(int pancakeSize, String instance) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("./input/pancakes/generated-"+ pancakeSize+ "/"+instance+".in"));
        Pancakes puzzle = new Pancakes(is);
        return puzzle;
    }

    public static SearchDomain createGridPathFinding(String mapName, String instance) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("./input/GridPathFinding/"+ mapName+ "/"+instance+".in"));
        GridPathFinding puzzle = new GridPathFinding(is);
        return puzzle;
    }

    public static SearchDomain createVacuumRobot(int numberOfDirts, String instance) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("./input/vacuumrobot/generated-"+ numberOfDirts+ "-dirt/"+instance+".in"));
        VacuumRobot puzzle = new VacuumRobot(is);
        return puzzle;
    }

    public static SearchDomain createDockyardRobot(String instance) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File("./input/dockyard-robot-max-edge-2-out-of-place-30/"+instance+".in"));
        DockyardRobot puzzle = new DockyardRobot(is);
        return puzzle;
    }

    /**
     * Runs A* to find the optimal solution to a givien problem
     */
    public static SearchResult findOptimalSolution(SearchDomain domain)
    {
        // Verify that ended up with the optimal solution (the same as A*)
        WAStar wastar = new WAStar();
        wastar.setAdditionalParameter("weight","1");
        SearchResult results = wastar.search(domain);
        junit.framework.Assert.assertTrue("Could not find the optimal solution", results.hasSolution());
        return results;
    }

    /**
     * Run a given search algorithm on a given domain and verify
     * the expected runtime, #generated, #expanded, cost, and length
     * are as expected.
     * @param domain
     * @param algo
     * @param generated
     * @param expanded
     * @param cost
     */
    public static void testSearchAlgorithm(SearchDomain domain, SearchAlgorithm algo,
                                    long generated, long expanded, double cost) {
        SearchResult result = algo.search(domain);
        SearchResult.Solution sol = result.getSolutions().get(0);
        Assert.assertTrue(result.getWallTimeMillis() > 1);
        Assert.assertTrue("Wall time is " + result.getWallTimeMillis(), result.getWallTimeMillis() < 20000);
        Assert.assertTrue(result.getCpuTimeMillis() > 1);
        Assert.assertTrue(result.getCpuTimeMillis() < 200);
        Assert.assertTrue(result.getGenerated() == generated);
        Assert.assertTrue(result.getExpanded() == expanded);
        Assert.assertTrue(sol.getCost() == cost);
        Assert.assertTrue(sol.getLength() == cost);
    }

    @Test
    public void testStub(){
        // This test is a santify check. Also, Gradle runs crazy if I have a Test class without a test (fails on build task)
        Assert.assertTrue(1==1);
    }
}
