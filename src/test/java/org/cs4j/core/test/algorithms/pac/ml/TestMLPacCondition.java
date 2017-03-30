package org.cs4j.core.test.algorithms.pac.ml;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.pac.conditions.MLPacCondition;
import org.cs4j.core.test.algorithms.TestUtils;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Roni Stern on 30/03/2017.
 */
public class TestMLPacCondition {

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

}
