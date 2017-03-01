package org.cs4j.core.test.algorithms.pac;

import junit.framework.Assert;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.domains.DockyardRobot;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.experiments.ExperimentUtils;
import org.junit.Test;

/**
 * Created by Roni Stern on 01/03/2017.
 */
public class TestOraclePACCondition {

    @Test
    public void testOraclePACCondition() {
        SearchDomain instance = ExperimentUtils.getSearchDomain(DockyardRobot.class, 87);
        PACSearchFramework psf = new PACSearchFramework();
        OraclePACCondition pacCondition = new OraclePACCondition();

        psf.setAnytimeSearchAlgorithm(new AnytimePTS4PAC());
        psf.setPACCondition(pacCondition);
        psf.setAdditionalParameter("epsilon", "1");
        psf.setAdditionalParameter("delta", "0");

        Double optimalSolution = PACUtils.getOptimalSolutions(instance.getClass()).get(87);
        pacCondition.setOptimalSolution(optimalSolution);

        SearchResult result = psf.search(instance);
        Assert.assertTrue(result.hasSolution());
    }
}
