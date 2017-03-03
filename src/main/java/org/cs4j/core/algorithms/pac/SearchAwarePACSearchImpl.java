package org.cs4j.core.algorithms.pac;

import org.cs4j.core.algorithms.AnytimeSearchNode;

import java.util.Comparator;

/**
 * Created by Roni Stern on 28/02/2017.
 *
 * A search-aware PAC search algorithm that is based on APTS
 */
public class SearchAwarePACSearchImpl extends SearchAwarePACSearch {
    @Override
    protected Comparator<AnytimeSearchNode> createNodeComparator() {
        return new Comparator<AnytimeSearchNode>(){
            public int compare(final AnytimeSearchNode a, final AnytimeSearchNode b) {
                double aCost = (SearchAwarePACSearchImpl.this.incumbentSolution - a.g) / a.h;
                double bCost = (SearchAwarePACSearchImpl.this.incumbentSolution - b.g) / b.h;

                if (aCost > bCost) {
                    return -1;
                }

                if (aCost < bCost) {
                    return 1;
                }

                // Here we have a tie @TODO: What about tie-breaking?
                return 0;
            }
        };
    }
}
