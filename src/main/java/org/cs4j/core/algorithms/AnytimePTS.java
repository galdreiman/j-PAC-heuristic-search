package org.cs4j.core.algorithms;


import org.cs4j.core.SearchResult;


import java.util.*;

/**
 * Created by user on 23/02/2017.
 * Anytime Potential Search
 *
 * @author Roni Stern (based on work by Vitali Sepetnitsky)
 */
public class AnytimePTS extends AbstractAnytimeSearch {

    public AnytimePTS() {
        super();
    }

    /**
     * Continues the search to find better goals
     * @return a better solution, if exists
     */
    @Override
    public SearchResult continueSearch() {
        // Resort open according to the new incumbent @TODO: Study if this actually helps or not?
        List<AnytimeSearchNode> openNodes = new ArrayList<AnytimeSearchNode>(this.open.size());
        while(this.open.size()>0)
            openNodes.add(this.open.poll());
        for(AnytimeSearchNode node : openNodes)
            this.open.add(node);
        openNodes.clear();

        return super.continueSearch();
    }


    /**
     * Create a node comparator used by the open list to prioritize the nodes
     */
    @Override
    protected Comparator<AnytimeSearchNode> createNodeComparator()
    {
        return new AnytimePTS.NodeComparator();
    }


    /**
     * The node comparator class
     */
    public class NodeComparator implements Comparator<AnytimeSearchNode> {
        @Override
        public int compare(final AnytimeSearchNode a, final AnytimeSearchNode b) {
            double aCost = (AnytimePTS.this.incumbentSolution - a.g) / a.h;
            double bCost = (AnytimePTS.this.incumbentSolution - b.g) / b.h;

            if (aCost > bCost) {
                return -1;
            }

            if (aCost < bCost) {
                return 1;
            }

            // Here we have a tie @TODO: What about tie-breaking?
            return 0;
        }
    }
}
