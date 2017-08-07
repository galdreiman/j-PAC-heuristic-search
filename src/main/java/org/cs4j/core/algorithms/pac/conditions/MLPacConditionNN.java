package org.cs4j.core.algorithms.pac.conditions;

import org.cs4j.core.SearchDomain;

/**
 * Created by Gal Dreiman on 25/06/2017.
 */
public class MLPacConditionNN extends MLPacCondition {

    static {
        MLPacCondition.clsType = "NN";
    }

    @Override
    public void setup(SearchDomain domain, double epsilon, double delta) {
        super.setup(domain, epsilon, delta);

    }
}
