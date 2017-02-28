package org.cs4j.core.test.algorithms.pac;

import junit.framework.Assert;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.pac.OpenBasedPACCondition;
import org.cs4j.core.domains.GridPathFinding;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.domains.VacuumRobot;
import org.cs4j.core.mains.DomainExperimentData;
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
        Map<Class<? extends SearchDomain>, double[]> classToHRanges = new HashMap<>();
        classToHRanges.put(GridPathFinding.class, new double[]{10,200,Double.MAX_VALUE});
        classToHRanges.put(Pancakes.class, new double[]{5,15,30,40,Double.MAX_VALUE});
        classToHRanges.put(VacuumRobot.class, new double[]{5,15,50,150,Double.MAX_VALUE});


        OpenBasedPACCondition condition = new OpenBasedPACCondition();

        Class[] domains = new Class[]{GridPathFinding.class};
        SortedMap<Double, SortedMap<Double,Double>> hRangeToCDF;
        double[] hRanges;
        Set<Double> observedHRanges;
        for(Class domainClass : domains) {
            hRanges = classToHRanges.get(domainClass);
            hRangeToCDF=condition.createCDFs(hRanges,
                    DomainExperimentData.get(domainClass).inputPath
                            + File.separator + "openBasedStatistics.csv");
            Assert.assertNotNull(hRangeToCDF);

            // Check h ranges didn't change
            observedHRanges = hRangeToCDF.keySet();
            Assert.assertEquals(observedHRanges.size(),hRanges.length);
            for(int i=0;i<hRanges.length;i++)
                Assert.assertTrue(observedHRanges.contains(hRanges[i]));
        }
    }
}
