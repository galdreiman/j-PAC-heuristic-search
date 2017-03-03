package org.cs4j.core.test.algorithms.pac;

import junit.framework.Assert;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.domains.GridPathFinding;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.domains.VacuumRobot;
import org.cs4j.core.experiments.ExperimentUtils;
import org.junit.Test;

/**
 * Created by Roni Stern on 03/03/2017.
 */
public class TestBoundedCost {

    @Test
    public void testStuck() {
        int instanceId = 87;
        double epsilon = 0.1;
        double delta = 0.25;
        Class conditionClass = TrivialPACCondition.class;
        SearchDomain instance = ExperimentUtils.getSearchDomain(Pancakes.class, instanceId);
        PACUtils.loadPACStatistics(Pancakes.class);
        PACSearchFramework psf = this.createPSF(epsilon, delta, conditionClass);

        SearchResult result = psf.search(instance);
        Assert.assertTrue(result.hasSolution());
    }



    /**
     * Create a PACSearchFramework instance with the given parameters
     */
    private PACSearchFramework createBCPSF(double epsilon, double delta, Class conditionClass){
        PACSearchFramework psf = new PACSearchFramework();
        psf.setAnytimeSearchClass(BoundedCostPACSearch.class);
        psf.setPACConditionClass(conditionClass);
        psf.setAdditionalParameter("delta",""+delta);
        psf.setAdditionalParameter("epsilon",""+epsilon);
        return psf;
    }

    /**
     * Create a PACSearchFramework instance with the given parameters
     */
    private PACSearchFramework createPSF(double epsilon, double delta, Class conditionClass){
        PACSearchFramework psf = new PACSearchFramework();
        psf.setAnytimeSearchClass(BoundedCostPACSearch.class);
        psf.setPACConditionClass(conditionClass);
        psf.setAdditionalParameter("delta",""+delta);
        psf.setAdditionalParameter("epsilon",""+epsilon);
        return psf;
    }
}
