package org.cs4j.core.test.algorithms.pac;

import junit.framework.Assert;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.DP;
import org.cs4j.core.algorithms.pac.OpenBasedPACCondition;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.SearchAwarePACSearchImpl;
import org.cs4j.core.domains.GridPathFinding;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.domains.VacuumRobot;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.test.algorithms.TestUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Roni Stern on 28/02/2017.
 */
public class TestOpenBasedPACCondition {


    /**
     * Tests building the CDFs from the gathered statistics
     */
    @Test
    public void testBuildingCDFs() throws IOException {
        OpenBasedPACCondition condition = new OpenBasedPACCondition();

        Class[] domains = new Class[]{GridPathFinding.class, Pancakes.class};
        SortedMap<Double, SortedMap<Double, Double>> hRangeToCDF;
        SearchDomain domain;
        for (Class domainClass : domains) {
            domain = ExperimentUtils.getSearchDomain(domainClass, 1);
            condition.setup(domain, 0, 0);
            hRangeToCDF = condition.hToCdf;
            Assert.assertNotNull(hRangeToCDF);
        }
    }


    @Test
    public void testPACSF() {
        SearchDomain instance = ExperimentUtils.getSearchDomain(Pancakes.class, 51);
        SearchAlgorithm dps = new DP("DPS", false, false, false); // A

        SearchResult result = dps.search(instance);
        Assert.assertTrue(result.hasSolution());


    }


    @Test
    public void testOpenBasedInSearch() {
        SearchDomain instance = ExperimentUtils.getSearchDomain(Pancakes.class, 51);
        PACSearchFramework psf = new PACSearchFramework();
        psf.setAnytimeSearchAlgorithm(new SearchAwarePACSearchImpl());
        psf.setPACCondition(new OpenBasedPACCondition());
        psf.setAdditionalParameter("epsilon", "1");
        psf.setAdditionalParameter("delta", "1");

        SearchResult result = psf.search(instance);
        Assert.assertTrue(result.hasSolution());
    }
}
