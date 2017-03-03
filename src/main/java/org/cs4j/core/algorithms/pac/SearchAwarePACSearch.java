package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AbstractAnytimeSearch;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.SearchResultImpl;

import java.util.*;

/**
 * Created by Roni Stern on 27/02/2017.
 *
 * The anytime search used when applying the Open-based PAC condition.
 * (1+epsilon)*h*(s) < incumbent
 * ==> exists a node n in OPEN s.t. (1+epsilon)*(g(n)+h*(n)) < incumbent
 * ==> not( for every node n in OPEN (1+epsilon)*(g(n)+h*(n)) > incumbent )
 * approximated by
 * 1-( product of (Pr((1+epsilon)*(g(n)+h*(n))>incumbent)
 */
abstract public class SearchAwarePACSearch extends AnytimePACSearch {
    private double epsilon; // @TODO: Consider if this is a good place for this




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
     * Expand a node, generating all its children
     *
     * @param currentNode the node being expanded
     * @return the goal node, if it was found. Otherwise, return null
     */
    @Override
    protected AnytimeSearchNode expand(AnytimeSearchNode currentNode) {
        SearchDomain.Operator op;
        SearchDomain.State childState;
        AnytimeSearchNode childNode;
        AnytimeSearchNode dupChildNode;
        double childf;
        double dupChildf;

        // Notify the SearchAware PAC condition that current node was popped from OPEN
        ((SearchAwarePACCondition)this.pacCondition).removedFromOpen(currentNode);

        // Extract a state from the node
        SearchDomain.State currentState = domain.unpack(currentNode.packed);
        for (int i = 0; i < domain.getNumOperators(currentState); ++i) {
            // Get the current operator
            op = domain.getOperator(currentState, i);
            // Don't apply the previous operator on the state - in order not to enter a loop
            if (op.equals(currentNode.pop)) {
                continue;
            }
            // Otherwise, let's generate the child state
            ++result.generated;
            // Get it by applying the operator on the parent state
            childState = domain.applyOperator(currentState, op);
            // Create a search node for this state
            childNode = new AnytimeSearchNode(this.domain,
                    childState, currentNode, currentState, op, op.reverse(currentState));

            // Prune nodes over the bound
            childf = childNode.getF();
            if (childf*(1+epsilon) >= this.incumbentSolution) {  // @TODO: Is it a good place to do this?
                continue;
            }

            // If the generated node satisfies the goal condition - let' mark the goal and break
            if (domain.isGoal(childState)) { // @TODO: Assumption: an edge is the lowest cost way to reach a node
                return childNode;
            }

            // If we got here - the state isn't a goal!

            // Now, merge duplicates - let's check if the state already exists in CLOSE/OPEN:
            // In the node is not in the CLOSED list, then it is also not in the OPEN list
            // In any case it can't be that node is a goal - otherwise, we should return it
            // when we see it at first
            if (this.closed.containsKey(childNode.packed)) {
                // Count the duplicates
                ++result.duplicates;
                // Take the duplicate node
                dupChildNode = this.closed.get(childNode.packed);
                childf = childNode.getF();
                dupChildf = dupChildNode.getF();
                if (dupChildf > childf) {
                    // Consider only duplicates with higher G value (i.e., the case where we now found a better path to it)
                    if (dupChildNode.g > childNode.g) {
                        boolean isInOpen = dupChildNode.getIndex(this.open.getKey()) != -1;
                        if(isInOpen) // Need to update its g value
                            ((SearchAwarePACCondition)this.pacCondition).removedFromOpen(dupChildNode);

                        // Make the duplicate to be successor of the current parent node
                        dupChildNode.g = childNode.g;
                        dupChildNode.op = childNode.op;
                        dupChildNode.pop = childNode.pop;
                        dupChildNode.parent = childNode.parent;

                        // In case the node is in the OPEN list - update its key using the new G
                        if (isInOpen) {
                            ++result.opupdated;
                            this.open.update(dupChildNode);
                            this.closed.put(dupChildNode.packed, dupChildNode);

                            // Update fCounter (and possible minf and maxminf)
                            this.addTofCounter(childf);
                            ((SearchAwarePACCondition)this.pacCondition).addedToOpen(dupChildNode);
                            this.removeFromfCounter(dupChildf);
                        }
                        else {
                            // Return to OPEN list only if reopening is allowed
                            if (this.reopen) {
                                ++result.reopened;
                                this.open.add(dupChildNode);
                                this.addTofCounter(childf);
                                ((SearchAwarePACCondition)this.pacCondition).addedToOpen(childNode);

                            } else {
                                // Maybe, we will want to expand these states later
                                this.incons.put(dupChildNode.packed, dupChildNode);
                            }
                            // In any case, update the duplicate node in CLOSED
                            this.closed.put(dupChildNode.packed, dupChildNode);
                        }
                    }
                }
                // Consider the new node only if its cost is lower than the maximum cost
            } else {
                // Otherwise, add the node to the search lists
                this.open.add(childNode);

                // Check the PAC condition
                this.addTofCounter(childNode.getF());
                ((SearchAwarePACCondition)this.pacCondition).addedToOpen(childNode);
                this.closed.put(childNode.packed, childNode);
            }
        }
        return null;
    }

    @Override
    protected void addNewIncumbent(SearchResult.Solution newSolution){
        super.addNewIncumbent(newSolution);

        // Update OPEN and PAC condition

        //@TODO: Replace this by defining an iterator over open instead of adding and removing all of the nodes in OPEN
        // Get all nodes in OPEN by removing all of them and then re-inserting them
        List<AnytimeSearchNode> openNodes = new ArrayList<>(this.open.size());
        while(this.open.size()>0) openNodes.add(this.open.poll());
        for(AnytimeSearchNode node : openNodes) this.open.add(node);

        // Update the PAC condition (this may throw PACCondition satisfied
        ((SearchAwarePACCondition) this.pacCondition).setIncumbent(
                this.totalSearchResults.getBestSolution().getCost(),openNodes);

        openNodes.clear(); // To free space. Probably this is not needed @TODO: Check if this helps memory and runtime
    }

}