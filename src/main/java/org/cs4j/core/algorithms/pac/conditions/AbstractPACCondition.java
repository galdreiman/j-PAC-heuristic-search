package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;

/**
 * Created by Roni Stern on 26/02/2017.
 */
public abstract class AbstractPACCondition implements PACCondition {
    protected SearchDomain domain;
    protected double epsilon;
    protected double delta;

    @Override
    public void setup(SearchDomain domain, double epsilon, double delta) {
        this.domain=domain;
        this.epsilon=epsilon;
        this.delta=delta;
    }

    public double getEpsilon() {return epsilon;}
    public double getDelta() {return delta;}
}
