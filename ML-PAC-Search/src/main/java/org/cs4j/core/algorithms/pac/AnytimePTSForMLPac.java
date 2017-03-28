package org.cs4j.core.algorithms.pac;

import org.apache.log4j.Logger;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.SearchResultImpl;

public class AnytimePTSForMLPac extends AnytimePTS{
	
	final static Logger logger = Logger.getLogger(AnytimePTSForMLPac.class);
	
	public AnytimePTSForMLPac() {
        super();
	}
	
	@Override
    public SearchResult continueSearch() {
		return super.continueSearch();
	}
	
	
    protected SearchResultImpl _search() {
        // The result will be stored here
        AnytimeSearchNode goal = null;
        this.result = new SearchResultImpl();
        if(this.totalSearchResults==null)
            this.totalSearchResults=this.result;

        result.startTimer();
        
        int hAndGCounter = 0;

         // Loop while there is no solution and there are states in the OPEN list
        AnytimeSearchNode currentNode;
        while ((goal == null) && !this.open.isEmpty()) {
            // Take a node from the OPEN list (nodes are sorted according to the 'u' function)
            currentNode = this.open.poll();
            this.removeFromfCounter(currentNode.getF());

            // expand the node (since, if its g satisfies the goal test - it would be already returned)
            goal = expand(currentNode);
            ++result.expanded;
            if (result.expanded % 1000000 == 0)
                logger.info("[INFO] Expanded so far " + result.expanded + ", incumbent ="+this.incumbentSolution+", fmin="+this.fmin+",opensize="+this.open.size());

            if(currentNode.getF()==this.fmin)
                this.updateFmin();
            
            if(hAndGCounter < 3){
            	this.result.setExtras("g_" + hAndGCounter, new Double(currentNode.g));
            	this.result.setExtras("h_"+hAndGCounter, new Double(currentNode.h));
            	++hAndGCounter;
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
    
}
