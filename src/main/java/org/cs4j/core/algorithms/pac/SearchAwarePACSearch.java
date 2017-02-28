package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
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
        // The result will be stored here
        AnytimeSearchNode goal = null;
        this.result = new SearchResultImpl();
        if(this.totalSearchResults==null)
            this.totalSearchResults=this.result;
        result.startTimer();


        // Loop while there is no solution and there are states in the OPEN list
        AnytimeSearchNode currentNode;
        while ((goal == null) && !this.open.isEmpty()) {
            // Take a node from the OPEN list (nodes are sorted according to the 'u' function)
            currentNode = this.open.poll();
            this.removeFromfCounter(currentNode.getF());
            ((SearchAwarePACCondition)this.pacCondition).removedFromOpen(currentNode);

            // expand the node (since, if its g satisfies the goal test - it would be already returned)
            goal = expand(currentNode);
            ++result.expanded;
            if (result.expanded % 1000000 == 0)
                logger.info("Expanded so far " + result.expanded);

            // Update fmin and prob-not-suboptimal
            this.updateFmin();

            // Check the open-based PAC condition
            if(this.pacCondition.shouldStop(this.result)){
                throw new PACConditionSatisfied(this.pacCondition);
            }
        }
        // Stop the timer and check that a goal was found
        result.stopTimer();

        // If a goal was found: update the solution
        if (goal != null) {
            result.addSolution(constructSolution(goal, this.domain));
        }

        result.setExtras("fmin",this.maxFmin); // Record the lower bound for future analysis @TODO: Not super elegant
        return result;
    }

    /**
     * Expand a node, generating all its children
     *
     * @param currentNode the node being expanded
     * @return the goal node, if it was found. Otherwise, return null
     */
    private AnytimeSearchNode expand(AnytimeSearchNode currentNode) {
        SearchDomain.Operator op;
        SearchDomain.State childState;
        AnytimeSearchNode childNode;
        AnytimeSearchNode dupChildNode;
        double childf;
        double dupChildf;

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
            if (childNode.getF() >= this.incumbentSolution) {
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
    /**
     * Continue the search. This means the previous solution was not PAC.
     * Here, we update the pac condition that a new incumbent solution was found.
     */
    public SearchResult continueSearch() {
        // Resort open according to the new incumbent @TODO: Study if this actually helps or not?
        List<AnytimeSearchNode> openNodes = new ArrayList<AnytimeSearchNode>(this.open.size());
        while(this.open.size()>0)
            openNodes.add(this.open.poll());
        for(AnytimeSearchNode node : openNodes) {
            this.open.add(node);
        }
        ((SearchAwarePACCondition)this.pacCondition).setIncumbent(this.totalSearchResults.getBestSolution().getCost(),
                openNodes);
        openNodes.clear(); // To free space. Probably this is not needed @TODO: Check if this helps memory and runtime
        return super.continueSearch();
    }



    /**
     * If there are no more nodes with the old fmin, need to update fmin and maybe also maxfmin accordingly.
     */
    @Override
    protected void updateFmin(){
        // If fmin is no longer fmin, need to search for a new fmin @TODO: May improve efficiency
        if(this.fCounter.containsKey(fmin)==false){
            fmin=Double.MAX_VALUE;
            for(double fInOpen : this.fCounter.keySet()){
                if(fInOpen<fmin)
                    fmin=fInOpen;
            }
            if(maxFmin<fmin) {
                maxFmin = fmin;
                ((SearchAwarePACCondition)this.pacCondition).setFmin(maxFmin);
            }
        }
    }
}