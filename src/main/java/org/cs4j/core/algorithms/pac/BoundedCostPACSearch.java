package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.SearchQueueElementImpl;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.collections.BucketHeap;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.collections.SearchQueueElement;

import java.util.Comparator;

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
