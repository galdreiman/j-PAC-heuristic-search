package org.cs4j.core.algorithms.pac;

import org.cs4j.core.algorithms.pac.conditions.OpenBasedPACCondition;

/**
 * This exception is fired when a PAC condition is satisfied
 */
public class PACConditionSatisfied extends RuntimeException{
	public PACCondition conditionSatisfied;

    public PACConditionSatisfied(PACCondition conditionSatisfied){
        super();
        this.conditionSatisfied=conditionSatisfied;
    }


    @Override
    public String toString(){
        String text=this.conditionSatisfied.getClass().getSimpleName();
        if(this.conditionSatisfied instanceof OpenBasedPACCondition)
            text=text+((OpenBasedPACCondition) this.conditionSatisfied).conditionFired;

        return text;
    }
}
