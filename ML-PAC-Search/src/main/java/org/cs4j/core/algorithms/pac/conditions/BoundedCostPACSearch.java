package org.cs4j.core.algorithms.pac.conditions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.algorithms.pac.AnytimePACSearch;
import org.cs4j.core.algorithms.pac.FMinCondition;
import org.cs4j.core.algorithms.pac.PACCondition;
import org.cs4j.core.algorithms.pac.PACConditionSatisfied;

/**
 * Created by Roni Stern on 03/03/2017.
 */
public class BoundedCostPACSearch extends AnytimePACSearch {

    private double threshold;

    public void setPacCondition(PACCondition pacCondition){
        super.setPacCondition(pacCondition);
    }

    /**
     * The internal main search procedure
     *
     * @return The search result filled by all the results of the search
     */
    protected SearchResultImpl _search() {
        this.threshold = ((ThresholdPACCondition)this.pacCondition).threshold;
        return super._search();
    }

    /**
     * If there are no more nodes with the old fmin, need to update fmin and maybe also maxfmin accordingly.
     */
    @Override
    protected void updateFmin(){
        super.updateFmin();

        if(this.maxFmin*(1+this.epsilon)>this.threshold) {
            this.threshold=this.maxFmin*(1+this.epsilon);
            this.resortOpen();//throw new RuntimeException("bip");
        }

        // Check PAC condition if max f min is updated
        if(this.totalSearchResults!=null) {
            if (this.incumbentSolution<=this.maxFmin * (1 + this.epsilon)){
                throw new PACConditionSatisfied(new FMinCondition());
            }
        }
    }

    private void resortOpen() {
        //@TODO: Replace this by defining an iterator over open instead of adding and removing all of the nodes in OPEN
        // Get all nodes in OPEN by removing all of them and then re-inserting them
        List<AnytimeSearchNode> openNodes = new ArrayList<>(this.open.size());
        while(this.open.size()>0) openNodes.add(this.open.poll());
        for(AnytimeSearchNode node : openNodes) this.open.add(node);
        openNodes.clear(); // To free space. Probably this is not needed @TODO: Check if this helps memory and runtime
    }

    @Override
    protected Comparator<AnytimeSearchNode> createNodeComparator() {
        return new Comparator<AnytimeSearchNode>() {
            @Override
            public int compare(AnytimeSearchNode a, AnytimeSearchNode b) {
                double aCost = (BoundedCostPACSearch.this.threshold - a.g) / a.h;
                double bCost = (BoundedCostPACSearch.this.threshold - b.g) / b.h;

                if (aCost > bCost) {
                    return -1;
                }

                if (aCost < bCost) {
                    return 1;
                }

                // Here we have a tie
                return 0;
            }
        };

    }
}
