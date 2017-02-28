package org.cs4j.core.algorithms.pac;

import org.cs4j.core.algorithms.AnytimeSearchNode;

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
     * @param incumbent The cost of the new incumbent solutions
     * @param openNodes The nodes in the open list
     */
    public void setIncumbent(double incumbent, List<AnytimeSearchNode> openNodes);
}
