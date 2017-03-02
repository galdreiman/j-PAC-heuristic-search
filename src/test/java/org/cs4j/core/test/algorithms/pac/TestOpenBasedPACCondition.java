package org.cs4j.core.test.algorithms.pac;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.DP;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.domains.DockyardRobot;
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

    final static Logger logger = Logger.getLogger(TestOpenBasedPACCondition.class);

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
    public void testOpenBasedInSearch() {
        SearchDomain instance = ExperimentUtils.getSearchDomain(Pancakes.class, 51);
        PACSearchFramework psf = new PACSearchFramework();
        psf.setAnytimeSearchClass(SearchAwarePACSearchImpl.class);
        psf.setPACConditionClass(OpenBasedPACCondition.class);
        psf.setAdditionalParameter("epsilon", "1");
        psf.setAdditionalParameter("delta", "1");

        SearchResult result = psf.search(instance);
        Assert.assertTrue(result.hasSolution());
    }


    @Test
    public void testOpenBasedInSearch2() {
        SearchDomain instance = ExperimentUtils.getSearchDomain(Pancakes.class, 51);
        PACSearchFramework psf = new PACSearchFramework();
        psf.setAnytimeSearchClass(SearchAwarePACSearchImpl.class);
        psf.setPACConditionClass(OpenBasedPACCondition.class);
        psf.setAdditionalParameter("epsilon", "1");
        psf.setAdditionalParameter("delta", "1");

        SearchResult result = psf.search(instance);
        Assert.assertTrue(result.hasSolution());
    }


    @Test
    public void testDeltaEffect(){
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
                    psf.setAnytimeSearchClass(SearchAwarePACSearchImpl.class);
                    psf.setPACConditionClass(OpenBasedPACCondition.class);
                    psf.setAdditionalParameter("delta",""+delta);
                    psf.setAdditionalParameter("epsilon",""+epsilon);

                    instance = ExperimentUtils.getSearchDomain(domainClass, instanceId); // Arbitrary instance
                    result = psf.search(instance);

                    Assert.assertTrue(result.hasSolution());
                    newExpanded = result.getExpanded();
                    Assert.assertTrue("oldExpanded="+oldExpanded+", newExpanded="+newExpanded,
                            oldExpanded>=newExpanded);
                    oldExpanded = result.getExpanded();
                }

            }
        }
    }
}
