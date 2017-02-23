package org.cs4j.core.algorithms;

import org.cs4j.core.AnytimeSearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.collections.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by user on 23/02/2017.
 * Anytime Potential Search
 *
 * @author Roni Stern (based on work by Vitali Sepetnitsky)
 */
public class AnytimePTS implements AnytimeSearchAlgorithm {

    // The domain to which the search problem belongs
    private SearchDomain domain;

    // OPEN and CLOSED lists
    private SearchQueue<Node> open;
    private Map<PackedElement, Node> closed;

    // Inconsistent list
    private Map<PackedElement, Node> incons;

    // The cost of the best solution found so far
    private double incumbentSolution;

    // Whether reopening is allowed
    private boolean reopen;

    private static final Map<String, Class> POSSIBLE_PARAMETERS;

    // Declare the parameters that can be tunes before running the search
    static
    {
        POSSIBLE_PARAMETERS = new HashMap<>();
    }

    public AnytimePTS() {
        // Initial values (afterwards they can be set independently)
        this.reopen = true;
    }

    @Override
    public String getName() {
        return "AnytimePTS";
    }

    /**
     * Initializes the data structures of the search
     *
     * @param clearOpen Whether to initialize the open list
     * @param clearClosed Whether to initialize the closed list
     */
    private void _initDataStructures(boolean clearOpen, boolean clearClosed) {
        if (clearOpen) {
            this.open = new BinHeap<>(new AnytimePTS.NodeComparator(), 0);
        }
        if (clearClosed) {
            this.closed = new HashMap<>();
        }
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return AnytimePTS.POSSIBLE_PARAMETERS;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            default: {
                System.err.println("No such parameter: " + parameterName + " (value: " + value + ")");
                throw new NotImplementedException();
            }
        }
    }

    /**
     * The internal main search procedure
     *
     * @return The search result filled by all the results of the search
     */
    private SearchResult _search() {
        // The result will be stored here
        Node goal = null;
        SearchResultImpl result = new SearchResultImpl();
        result.startTimer();

        // Extract the initial state from the domain
        SearchDomain.State currentState = domain.initialState();
        // Initialize a search node using the state (contains data according to the current
        // algorithm)
        Node initialNode = new Node(currentState);

        // Start the search: Add the node to the OPEN and CLOSED lists
        this.open.add(initialNode);
        // n in OPEN ==> n in CLOSED -Thus- ~(n in CLOSED) ==> ~(n in OPEN)
        this.closed.put(initialNode.packed, initialNode);

        // Loop while there is no solution and there are states in the OPEN list
        SearchDomain.State childState;
        Node currentNode,childNode,dupChildNode;
        SearchDomain.Operator op;
        while ((goal == null) && !this.open.isEmpty()) {
            // Take a node from the OPEN list (nodes are sorted according to the 'u' function)
            currentNode = this.open.poll();
            // Extract a state from the node
            currentState = domain.unpack(currentNode.packed);
            // expand the node (since, if its g satisfies the goal test - it would be already returned)
            ++result.expanded;
            // Go over all the successors of the state
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
                childNode = new Node(childState, currentNode, currentState, op, op.reverse(currentState));

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

                // Now, merge duplicates - let's check if the state already exists in CLOSE/OPEN:
                // In the node is not in the CLOSED list, then it is also not in the OPEN list
                // In any case it can't be that node is a goal - otherwise, we should return it
                // when we see it at first
                if (this.closed.containsKey(childNode.packed)) {
                    // Count the duplicates
                    ++result.duplicates;
                    // Take the duplicate node
                    dupChildNode = this.closed.get(childNode.packed);
                    if (dupChildNode.getF() > childNode.getF()) {
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
                            } else {
                                // Return to OPEN list only if reopening is allowed
                                if (this.reopen) {
                                    ++result.reopened;
                                    this.open.add(dupChildNode);
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
                    this.closed.put(childNode.packed, childNode);
                }
            }
        }
        // Stop the timer and check that a goal was found
        result.stopTimer();

        // If a goal was found: update the solution
        if (goal != null) {
            result.addSolution(constructSolution(goal, this.domain));
        }

        return result;
    }


    /**
     * Construct a solution for the given domain after a goal has been found,
     * and update the given SearchResults object accordingly.
     * @param goal The goal node that was found
     * @param domain The domain
     * @return The new solution found
     */
    private static SearchResult.Solution constructSolution(Node goal, SearchDomain domain) {
        Node currentNode;
        SearchResultImpl.SolutionImpl solution = new SearchResultImpl.SolutionImpl(domain);
        List<SearchDomain.Operator> path = new ArrayList<>();
        List<SearchDomain.State> statesPath = new ArrayList<>();
        System.out.println("[INFO] Solved - Generating output path.");
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
        this.incumbentSolution=Double.MAX_VALUE;
        // The result will be stored here
        // Initialize all the data structures )
        this._initDataStructures(true, true);
        SearchResult results = this._search();
        if(results.hasSolution())
            this.incumbentSolution=results.getSolutions().get(0).getCost();
        return results;
    }

    /**
     * Continues the search to find better goals
     * @return a better solution, if exists
     */
    @Override
    public SearchResult continueSearch() {
        this._initDataStructures(false,false);
        SearchResult results = this._search();
        if(results.hasSolution()) {
            double solutionCost = results.getSolutions().get(0).getCost();
            assert solutionCost<this.incumbentSolution;
            this.incumbentSolution = solutionCost;
        }
        return results;
    }

    /**
     * The Node is the basic data structure which is used by the algorithm during the search -
     * OPEN and CLOSED lists contain nodes which are created from the domain states
     */
    private final class Node extends SearchQueueElementImpl implements BucketHeap.BucketHeapElement {
        private double g;
        private double h;
        private double d;

        private SearchDomain.Operator op;
        private SearchDomain.Operator pop;

        private Node parent;

        private PackedElement packed;

        private int[] secondaryIndex;

        /**
         * An extended constructor which receives the initial state, but also the parent of the node
         * and operators (last and previous)
         *
         * @param state The state from which the node should be created
         * @param parent The parent node
         * @param parentState The state of the parent
         * @param op The operator which was applied to the parent state in order to get the current
         *           one
         * @param pop The operator which will reverse the last applied operation which revealed the
         *            current state
         */
        private Node(SearchDomain.State state, Node parent, SearchDomain.State parentState, SearchDomain.Operator op, SearchDomain.Operator pop) {
            // The size of the key (for SearchQueueElementImpl) is 1
            super(1);
            this.secondaryIndex = new int[1];
            // WHY THE COST IS OF APPLYING THE OPERATOR ON THAT NODE????
            // SHOULDN'T IT BE ON THE PARENT???
            // OR EVEN MAYBE WE WANT EITHER PARENT **AND** THE CHILD STATES TO PASS TO THE getCost
            // FUNCTION IN ORDER TO GET THE OPERATOR VALUE ...
            double cost = (op != null) ? op.getCost(state, parentState) : 0;
            this.h = state.getH();
            this.d = state.getD();
            this.g = (parent != null)? parent.g + cost : cost;
            this.parent = parent;
            this.packed = domain.pack(state);
            this.pop = pop;
            this.op = op;
        }

        /**
         * @return The computed (on the fly) value of f
         */
        public double getF() {
            return this.g + this.h;
        }

        @Override
        public double getG() {
            return this.g;
        }

        @Override
        public double getDepth() {
            return 0;
        }

        @Override
        public double getH() {
            return this.h;
        }

        @Override
        public double getD() {return this.d;}

        @Override
        public double getHhat() {
            return 0;
        }

        @Override
        public double getDhat() {
            return 0;
        }

        @Override
        public SearchQueueElement getParent() {return this.parent;}

        /**
         * Default constructor which creates the node from some given state
         *
         * {see Node(State, Node, State, Operator, Operator)}
         *
         * @param state The state from which the node should be created
         */
        private Node(SearchDomain.State state) {
            this(state, null, null, null, null);
        }

        @Override
        public void setSecondaryIndex(int key, int index) {
            this.secondaryIndex[key] = index;
        }

        @Override
        public int getSecondaryIndex(int key) {
            return this.secondaryIndex[key];
        }

        @Override
        public double getRank(int level) {
            return (level == 0) ? this.getF() : this.g;
        }
    }

    /**
     * The node comparator class
     */
    private final class NodeComparator implements Comparator<Node> {
        @Override
        public int compare(final Node a, final Node b) {
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
