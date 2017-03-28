package org.cs4j.core.algorithms.pac.conditions;

import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.pac.PACCondition;

import java.util.List;

/**
 * Created by Roni Stern on 28/02/2017.
 *
 * A PAC condition that is search-aware
 */
public interface SearchAwarePACCondition extends PACCondition {

    public void removedFromOpen(AnytimeSearchNode node);
    public void addedToOpen(AnytimeSearchNode node);
    /**
     * Fmin has been updated. Check if a PAC conddition is satisfied
     * @param fmin
     */
    public void setFmin(double fmin);

    /**
     * A new incumbent solution has been found.
     * @param newSearchResults The new search results
     * @param openNodes The nodes in the open list
     */
    public void addNewSearchResults(SearchResult newSearchResults, List<AnytimeSearchNode> openNodes);
}
