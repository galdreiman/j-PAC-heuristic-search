package org.cs4j.core.algorithms;

import org.apache.log4j.Logger;
import org.cs4j.core.AnytimeSearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.collections.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Roni Stern on 23/02/2017.
 */
public abstract class AbstractAnytimeSearch implements AnytimeSearchAlgorithm {
    final static Logger logger = Logger.getLogger(AbstractAnytimeSearch.class);

    // The domain to which the search problem belongs
    protected SearchDomain domain;

    // OPEN and CLOSED lists
//    protected SearchQueue<Node> open;
    protected SearchQueue<AnytimeSearchNode> open;//gh_heap

    protected Map<PackedElement, AnytimeSearchNode> closed;

    // Inconsistent list
    protected Map<PackedElement, AnytimeSearchNode> incons;

    // The search results encompasses all the iterations run so far
    protected SearchResultImpl totalSearchResults;

    // The search result of the current iteration
    protected SearchResultImpl result;

    // The cost of the best solution found so far
    public double incumbentSolution;

    // The number of anytime iteratoins (~ the number of goals found so far)
    public int iteration;

    // Whether reopening is allowed
    protected boolean reopen;


    // A data structure to maintain minf. @TODO: Allow disabling this for Anytime algorithms that don't care about this
    protected HashMap<Double, Integer> fCounter = new HashMap<Double,Integer>();
    protected double maxFmin; // The maximal fmin observed so far. This is a lower bound on the optimal cost

    // The minimal f value currently in the open list.
    // Actually, this holds an f value of some nodes in OPEN, such that this f value is smaller than max f min.
    // Its purpose is to know when to go over OPEN and try to increase max f min.
    protected double fmin;

    public AbstractAnytimeSearch() {
        // Initial values (afterwards they can be set independently)
        this.reopen = true;
    }

    @Override
    public String getName(){
        return this.getClass().getSimpleName();
    }


    /**
     * Initializes the data structures of the search
     *
     * @param clearOpen   Whether to initialize the open list
     * @param clearClosed Whether to initialize the closed list
     */
    protected void _initDataStructures(boolean clearOpen, boolean clearClosed) {
        if (clearOpen) {
            this.open = new BinHeap<AnytimeSearchNode>(this.createNodeComparator(), 0);
        }
        if (clearClosed) {
            this.closed = new HashMap<>();
        }
        this.fCounter.clear();
        this.totalSearchResults=null;
        this.fmin=-1;
        this.maxFmin=-1;
        this.result=null;
        this.incumbentSolution=Double.MAX_VALUE;
        this.iteration=0;
    }


    /**
     * Create a node comparator used by the open list to prioritize the nodes
     */
    abstract protected Comparator<AnytimeSearchNode> createNodeComparator();


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

            // expand the node (since, if its g satisfies the goal test - it would be already returned)
            goal = expand(currentNode);
            ++result.expanded;
            if (result.expanded % 1000000 == 0)
                logger.info("[INFO] Expanded so far " + result.expanded);

            if(currentNode.getF()==this.fmin)
                this.updateFmin();
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
     * Expands a node and generate its children
     */
    protected AnytimeSearchNode expand(AnytimeSearchNode currentNode) {
        SearchDomain.Operator op;
        SearchDomain.State childState;
        AnytimeSearchNode childNode;
        double childf;
        AnytimeSearchNode dupChildNode;
        double dupChildf;
        AnytimeSearchNode goal = null;

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
                    childState,
                    currentNode,
                    currentState,
                    op, op.reverse(currentState));

            // Prune nodes over the bound
            if (childNode.getF() >= this.incumbentSolution) {
                continue;
            }

            // If the generated node satisfies the goal condition - let' mark the goal and break
            if (domain.isGoal(childState)) {
                goal = childNode;
                break;
            }

            // If we got here - the state isn't a goal!
            childf = childNode.getF();


            // Now, merge duplicates - let's check if the state already exists in CLOSE/OPEN:
            // In the node is not in the CLOSED list, then it is also not in the OPEN list
            // In any case it can't be that node is a goal - otherwise, we should return it
            // when we see it at first
            if (this.closed.containsKey(childNode.packed)) {
                // Count the duplicates
                ++result.duplicates;
                // Take the duplicate node
                dupChildNode = this.closed.get(childNode.packed);
                dupChildf = dupChildNode.getF();
                if (dupChildf > childf) {
                    // Consider only duplicates with higher G value
                    if (dupChildNode.g > childNode.g) {
                        // Make the duplicate to be successor of the current parent node
                        dupChildNode.g = childNode.g;
                        dupChildNode.op = childNode.op;
                        dupChildNode.pop = childNode.pop;
                        dupChildNode.parent = childNode.parent;

                        // In case the node is in the OPEN list - update its key using the new G
                        if (dupChildNode.getIndex(this.open.getKey()) != -1) {
                            ++result.opupdated;
                            this.open.update(dupChildNode);
                            this.closed.put(dupChildNode.packed, dupChildNode);

                            // Update fCounter (and possible minf and maxminf)
                            this.addTofCounter(childf);
                            this.removeFromfCounter(dupChildf);
                        } else {
                            // Return to OPEN list only if reopening is allowed
                            if (this.reopen) {
                                ++result.reopened;
                                this.open.add(dupChildNode);
                                this.addTofCounter(childf);

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
                this.addTofCounter(childNode.getF());
                this.closed.put(childNode.packed, childNode);
            }
        }
        return goal;
    }


    /**
     * After adding a node to OPEN, we update the f-counter
     * to keep track of minf
     * @param f the (admissible) f value of the node that was just added to OPEN
     */
    protected void addTofCounter(double f){
        if(this.fCounter.containsKey(f))
            this.fCounter.put(f, this.fCounter.get(f)+1);
        else
            this.fCounter.put(f,1);

        // Update fmin if needed
        if(f<this.fmin)
            this.fmin=f;
    }
    /**
     * After removing from OPEN a node with a given f-value,
     */
    protected void removeFromfCounter(double f) {
        int newfCount = this.fCounter.get(f)-1;
        this.fCounter.put(f,newfCount);

        if(newfCount==0){
            this.fCounter.remove(f);
        }
    }

    /**
     * If there are no more nodes with the old fmin, need to update fmin and maybe also maxfmin accordingly.
     */
    protected void updateFmin(){
        // If fmin is no longer fmin, need to search for a new fmin @TODO: May improve efficiency
        if(this.fCounter.containsKey(fmin)==false){
            fmin=Double.MAX_VALUE;
            for(double fInOpen : this.fCounter.keySet()){
                if(fInOpen<fmin)
                    fmin=fInOpen;
            }
            if(maxFmin<fmin)
                maxFmin=fmin;
        }
    }



    /**
     * Construct a solution for the given domain after a goal has been found,
     * and update the given SearchResults object accordingly.
     * @param goal The goal node that was found
     * @param domain The domain
     * @return The new solution found
     */
    protected static SearchResult.Solution constructSolution(AnytimeSearchNode goal, SearchDomain domain) {
        AnytimeSearchNode currentNode;
        SearchResultImpl.SolutionImpl solution = new SearchResultImpl.SolutionImpl(domain);
        List<SearchDomain.Operator> path = new ArrayList<>();
        List<SearchDomain.State> statesPath = new ArrayList<>();
        double cost = 0;

        SearchDomain.State currentPacked = domain.unpack(goal.packed);
        SearchDomain.State currentParentPacked = null;
        for (currentNode = goal;
             currentNode != null;
             currentNode = currentNode.parent, currentPacked = currentParentPacked) {
            // If op of current node is not null that means that p has a parent
            if (currentNode.op != null) {
                path.add(currentNode.op);
                currentParentPacked = domain.unpack(currentNode.parent.packed);
                cost += currentNode.op.getCost(currentPacked, currentParentPacked);
            }
            statesPath.add(domain.unpack(currentNode.packed));
        }
        logger.info("[INFO] Solved - Generating output path. Cost="+cost);
        // The actual size of the found path can be only lower the G value of the found goal
        assert cost <= goal.g;
        if (cost - goal.g < 0) {
            System.out.println("[INFO] Goal G is higher that the actual cost " +
                    "(G: " + goal.g +  ", Actual: " + cost + ")");
        }

        Collections.reverse(path);
        solution.addOperators(path);

        Collections.reverse(statesPath);
        solution.addStates(statesPath);

        solution.setCost(cost);
        return solution;
    }

    /**
     * Search from a given start node until finding the first goal
     * @param domain The domain to apply the search on
     */
    @Override
    public SearchResult search(SearchDomain domain) {
        // Initially all the data structures are cleaned
        this.domain = domain;

        // The result will be stored here
        // Initialize all the data structures )
        this._initDataStructures(true, true);

        // Create the initial node and add it to OPEN
        SearchDomain.State currentState = domain.initialState();
        AnytimeSearchNode initialNode = new AnytimeSearchNode(this.domain,currentState);
        this.open.add(initialNode);

        // Set the min f counters to the f value of the initial node
        double startFmin = initialNode.getF();
        this.fCounter.put(startFmin,1);
        this.fmin=startFmin;
        this.maxFmin=this.fmin;

        // n in OPEN ==> n in CLOSED -Thus- ~(n in CLOSED) ==> ~(n in OPEN)
        this.closed.put(initialNode.packed, initialNode);

        // Run the search!
        SearchResult result = this._search();
        if(result.hasSolution())
            this.addNewIncumbent(result.getBestSolution());

        return result;
    }


    /**
     * Continues the search to find better goals
     * @return a better solution, if exists
     */
    @Override
    public SearchResult continueSearch() {
        this.iteration++;
        SearchResult result = this._search();
        if(result.hasSolution()) {
            this.addNewIncumbent(result.getBestSolution());
        }
        this.totalSearchResults.addIteration(this.iteration,this.incumbentSolution,result.getExpanded(), result.getGenerated());
        this.totalSearchResults.increase(result);
        return result;
    }

    /**
     * Update data structures with the new result.
     * This is mainly the incumbent solution and totalSearchResults.
     * @param newSolution the new result found
     */
    protected void addNewIncumbent(SearchResult.Solution newSolution){
        double solutionCost = newSolution.getCost();
        assert solutionCost<this.incumbentSolution;
        this.incumbentSolution = solutionCost;
        if(this.totalSearchResults!=this.result) { // If this is not the first result returned, update the total search results
            // @TODO: Fix the SearchResults object to do this by itself in the addIteration function
            this.totalSearchResults.getSolutions().add(newSolution);
        }
    }



    @Override
    public Map<String, Class> getPossibleParameters() {
        return new HashMap<>();
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            default: {
                System.err.println("No such parameter: " + parameterName + " (value: " + value + ")");
                throw new UnsupportedOperationException();
            }
        }
    }
    /**
     * Returns a SearchResults object that contains all the search results so
     */
    @Override
    public SearchResult getTotalSearchResults() { return this.totalSearchResults; }

}
