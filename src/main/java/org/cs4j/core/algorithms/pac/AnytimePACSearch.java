package org.cs4j.core.algorithms.pac;

import org.apache.log4j.Logger;
import org.cs4j.core.algorithms.AbstractAnytimeSearch;
import org.cs4j.core.algorithms.SearchResultImpl;

/**
 * Created by Roni Stern on 26/02/2017.
 *
 * This is an anytime search algorithm that is aware that it is used in a PAC Search framework
 * and is intended to find a PAC solution.
 * This enables some faster halting, e.g., when f-min condition occurs.
 */
public abstract class AnytimePACSearch extends AbstractAnytimeSearch {
    final static Logger logger = Logger.getLogger(AnytimePACSearch.class);

    protected PACCondition pacCondition;
    protected double epsilon;
    public void setPacCondition(PACCondition pacCondition){
        this.pacCondition = pacCondition;
        this.epsilon=this.pacCondition.getEpsilon();
        this.delta = this.pacCondition.getDelta();
    }


    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            default: {
                logger.error("No such parameter: " + parameterName + " (value: " + value + ")");
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * The internal main search procedure
     *
     * @return The search result filled by all the results of the search
     */
    protected SearchResultImpl _search() {

        try{
            return super._search();
        }
        catch(PACConditionSatisfied exception){
            // If the PAC conditions was satisfied, prepare the results objects
            result.setExtras("fmin",this.maxFmin); // Record the lower bound for future analysis @TODO: Not super elegant
            result.setExtras("pac-condition-statisfied",exception.conditionSatisfied); // Record the lower bound for future analysis @TODO: Not super elegant
            result.stopTimer();

            this.totalSearchResults.increase(this.result);
            this.totalSearchResults.stopTimer();

            return result;
        }
    }


    /**
     * If there are no more nodes with the old fmin, need to update fmin and maybe also maxfmin accordingly.
     */
    @Override
    protected void updateFmin(){
        super.updateFmin();

        // Check PAC condition if max f min is updated
        if(this.totalSearchResults!=null) {
            if (this.incumbentSolution<=this.maxFmin * (1 + this.epsilon)){
                throw new PACConditionSatisfied(new FMinCondition());
            }
        }
    }


}
