package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimeSearchNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Roni Stern on 26/02/2017.
 *
 * An anytime PAC search that is based on APTS
 */
public class AnytimePTS4PAC extends AnytimePACSearch {
    @Override
    protected Comparator<AnytimeSearchNode> createNodeComparator() {
        return new Comparator<AnytimeSearchNode>(){
            public int compare(final AnytimeSearchNode a, final AnytimeSearchNode b) {
                double aCost = (AnytimePTS4PAC.this.incumbentSolution - a.g) / a.h;
                double bCost = (AnytimePTS4PAC.this.incumbentSolution - b.g) / b.h;

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


    /**
     * Resort OPEN before continuing the search, because the PTS evaluation function considers the incumbent solution
     * @param newSolution the new result found
     */
    @Override
    protected void addNewIncumbent(SearchResult.Solution newSolution){
        super.addNewIncumbent(newSolution);

        //@TODO: Replace this by defining an iterator over open instead of adding and removing all of the nodes in OPEN
        // Get all nodes in OPEN by removing all of them and then re-inserting them
        List<AnytimeSearchNode> openNodes = new ArrayList<>(this.open.size());
        while(this.open.size()>0) openNodes.add(this.open.poll());
        for(AnytimeSearchNode node : openNodes) this.open.add(node);
        openNodes.clear(); // To free space. Probably this is not needed @TODO: Check if this helps memory and runtime
    }
}
