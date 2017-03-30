package org.cs4j.core.test.algorithms.pac.ml;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.algorithms.pac.conditions.MLPacCondition;
import org.cs4j.core.domains.VacuumRobot;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.test.algorithms.TestUtils;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Roni Stern on 30/03/2017.
 */
public class TestMLPacCondition {

    private final static Logger logger = Logger.getLogger(TestMLPacCondition.class);

    @Test
    public void testSetupOneClass() throws IOException{
        SearchDomain instance = TestUtils.createVacuumRobot(5,"71");
        double delta = 0.2;
        double epsilon = 0.7;

        MLPacCondition condition = new MLPacCondition();

        condition.setup(instance, epsilon, delta);

    }


    @Test
    public void testSetup() throws IOException{
        SearchDomain instance = TestUtils.createVacuumRobot(5,"71");
        double delta = 0.2;
        double epsilon = 0.5;

        MLPacCondition condition = new MLPacCondition();

        condition.setup(instance, epsilon, delta);
    }

    @Test
    public void testDeltaEffect(){
        Class[] domains = {VacuumRobot.class};
        SearchDomain instance;
        PACSearchFramework psf;
        SearchResult result;
        int instanceId = 51;
        double oldExpanded=Double.MAX_VALUE;
        double newExpanded;
        for(Class domainClass :domains){
            logger.info("Testing domain " + domainClass.getName());
            for(Double epsilon : new Double[]{0.2,0.5}){
                for(Double delta : new Double[]{0.0,0.1, 0.25, 0.5, 0.75, 1.0}){
                    logger.info("Testing eps="+epsilon+",delta="+delta);
                    PACUtils.loadPACStatistics(domainClass);
                    psf = new PACSearchFramework();
                    psf.setAnytimeSearchClass(AnytimePTS4PAC.class);
                    psf.setPACConditionClass(MLPacCondition.class);
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
