package org.cs4j.core.test.algorithms.pac;

import org.cs4j.core.algorithms.pac.FMinCondition;
import org.cs4j.core.algorithms.pac.conditions.TrivialPACCondition;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.experiments.PACExperimentRunner;
import org.junit.Test;

/**
 * Created by Roni Stern on 02/03/2017.
 */
public class TestExperimentRunners {

    @Test
    public void testConditionsDifferent(){
        PACExperimentRunner runner = new PACExperimentRunner();
        Class[] domains=new Class[]{Pancakes.class};
        Class[] pacConditions = new Class[]{TrivialPACCondition.class, FMinCondition.class};
        double[] epsilonValues = new double[]{0.5,0.8};
        //runner.runThresholdBasedConditions(domains,pacConditions,epsilonValues);
    }
}
