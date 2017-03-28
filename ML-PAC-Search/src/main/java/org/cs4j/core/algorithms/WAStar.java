/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cs4j.core.algorithms;

import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchDomain.Operator;
import org.cs4j.core.SearchDomain.State;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.SearchResultImpl.SolutionImpl;
import org.cs4j.core.collections.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * A* Search and Weighted A* Search
 *
 * @author Matthew Hatem
 *
 * (Edited by Vitali Sepetnitsky)
 */
public class WAStar implements SearchAlgorithm {

	protected static final int QID = 0;

	protected static final Map<String, Class> WAStarPossibleParameters;

    // Declare the parameters that can be tuned before running the search
    static
    {
        WAStarPossibleParameters = new HashMap<>();
        WAStar.WAStarPossibleParameters.put("weight", Double.class);
        WAStar.WAStarPossibleParameters.put("reopen", Boolean.class);
        WAStar.WAStarPossibleParameters.put("max-cost", Double.class);
        WAStar.WAStarPossibleParameters.put("bpmx", Boolean.class);
    }

    // The domain for the search
    protected SearchDomain domain;
    // Open list (frontier)
    protected SearchQueue<Node> open;
//    private BinHeapF<Node> openF;
    // Closed list (seen states)
    protected TreeMap<PackedElement, Node> closed;
//    private Map<PackedElement, Node> closed;

    // TODO ...
    protected HeapType heapType;

    public enum HeapType {BIN, BUCKET}

    // For weighted A*
    protected double weight;
    // Whether to perform reopening of states
    protected boolean reopen;

    protected double maxCost;

    protected boolean useBPMX;

    protected int FR;

    protected SearchResultImpl result;

    /**
     * Sets the default values for the relevant fields of the algorithm
     */
    protected void _initDefaultValues() {
        // Default values
        this.weight = 1.0;
        this.reopen = true;
        this.maxCost = Double.MAX_VALUE;
        this.useBPMX = false;
        this.FR = Integer.MAX_VALUE;
    }


    /**
     * A constructor
     *
     * @param heapType the type of heap to use (BIN | BUCKET)
     *
     */
    protected WAStar(HeapType heapType) {
        this.heapType = heapType;
        this._initDefaultValues();
    }

    /**
     * A default constructor of the class (weight of 1.0, binary heap and AR)
     *
     */
    public WAStar() {
        this(HeapType.BIN);
    }

    @Override
    public String getName() {
        return "wastar";
    }

    /**
     * Creates a heap according to the required type (Builder design pattern)
     *
     * @param heapType Type of the required heap (choose from the available types)
     * @param size Initial size of the heap
     *
     * NOTE: In case of unknown type, null is returned (no exception is thrown)
     * @return The created heap
     */
    protected SearchQueue<Node> buildHeap(HeapType heapType, int size) {
        SearchQueue<Node> heap = null;
        switch (heapType) {
            case BUCKET:
//                heap = new BucketHeap<>(size, QID);
                break;
            case BIN:
                heap = new BinHeap<>(new NodeComparator(), 0);
                break;
        }
        return heap;
    }

    protected void _initDataStructures(SearchDomain domain) {
        this.domain = domain;
        this.open = new BinHeap<>(new NodeComparator(), 0);
//        this.openF = new BinHeapF<>(1,domain);
//        this.open = buildHeap(heapType, 100);
        this.closed = new TreeMap<>();
//        this.closed = new HashMap<>();
    }

    @Override
    public SearchResult search(SearchDomain domain) {
        // Initialize all the data structures required for the search
        this._initDataStructures(domain);
        Node goal = null;

        result = new SearchResultImpl();

        result.startTimer();

        // Let's instantiate the initial state
        State currentState = domain.initialState();
        // Create a graph node from this state
        Node initNode = new Node(currentState);

        // And add it to the frontier
        _addNode(initNode);
        try {
            // Loop over the frontier
            while (!this.open.isEmpty() && result.getGenerated() < this.domain.maxGeneratedSize() && result.checkMinTimeOut()) {
                // Take the first state (still don't remove it)
    //            Node currentNode = this.open.poll();
                Node currentNode = _selectNode();
                // Prune
                if (currentNode.getRf() >= this.maxCost) {
                    continue;
                }

                // Extract the state from the packed value of the node
                currentState = domain.unpack(currentNode.packed);

                //System.out.println(currentState.dumpStateShort());
                // Check for goal condition
                if (domain.isGoal(currentState)) {
                    goal = currentNode;
                    break;
                }

                List<Pair<State, Node>> children = new ArrayList<>();

                // Expand the current node
                ++result.expanded;
                // Stores parent h-cost (from path-max)
                double bestHValue = 0.0d;
                // First, let's generate all the children
                // Go over all the possible operators and apply them
                for (int i = 0; i < domain.getNumOperators(currentState); ++i) {
                    Operator op = domain.getOperator(currentState, i);
                    // Try to avoid loops
                    if (op.equals(currentNode.pop)) {
                        continue;
                    }
                    State childState = domain.applyOperator(currentState, op);
                    Node childNode = new Node(childState, currentNode, currentState, op, op.reverse(currentState));
                    // Here we actually generated a new state
                    ++result.generated;
/*                    if(result.getGenerated() % 1000 == 0){
                        DecimalFormat formatter = new DecimalFormat("#,###");
//                        System.out.println("[INFO] WA Generated:" + formatter.format(result.getGenerated()) + "\tTime:"+passed);
                        System.out.print("\r[INFO] WA Generated:" + formatter.format(result.getGenerated()));

                    }*/
                    // Perform only if BPMX is required
                    if (this.useBPMX) {
                        bestHValue = Math.max(bestHValue, childNode.h - op.getCost(childState, currentState));
                    }
                    children.add(new Pair<>(childState, childNode));
                }

                // Update the H Value of the parent in case of BPMX
                if (this.useBPMX) {
                    currentNode.h = Math.max(currentNode.h, bestHValue);
                    // Prune
                    if (currentNode.getRf() >= this.maxCost) {
                        continue;
                    }
                }

                // Go over all the possible operators and apply them
                for (Pair<State, Node> currentChild : children) {
                    State childState = currentChild.getKey();
                    Node childNode = currentChild.getValue();
                    double edgeCost = childNode.op.getCost(childState, currentState);

                    // Prune
                    if (childNode.getRf() >= this.maxCost) {
                        continue;
                    }
                    // Treat duplicates
                    boolean contains = true;
                    PackedElement p = childNode.packed;
                    Node testNode = this.closed.get(p);
                    if(testNode == null){
                        contains = false;
                    }
//                    contains = this.closed.containsKey(childNode.packed);
                    if (contains) {
                        // Count the duplicates
                        ++result.duplicates;
                        // Get the previous copy of this node (and extract it)
                        Node dupChildNode = this.closed.get(childNode.packed);

                        // Propagate the H value to child (in case of BPMX)
                        if (this.useBPMX) {
                            dupChildNode.h = Math.max(dupChildNode.h, currentNode.h - edgeCost);
                        }

                        // Found a shorter path to the node
                        if (dupChildNode.g > childNode.g) {
                            // Check that the f actually decreases
                            if (dupChildNode.getWf() > childNode.getWf()) {
                                // Do nothing
                            } else {
                                if (this.domain.isCurrentHeuristicConsistent()) {
                                    assert false;
                                }
                                continue;
                            }
                            // In any case update the duplicate with the new values - we reached it via a shorter path
                            double dupF = dupChildNode.getF();
                            dupChildNode.g = childNode.g;
                            dupChildNode.op = childNode.op;
                            dupChildNode.pop = childNode.pop;
                            dupChildNode.parent = childNode.parent;

                            // if dupChildNode is in open, update it there too
                            if (dupChildNode.getIndex(this.open.getKey()) != -1) {
                                ++result.opupdated;
                                this.open.update(dupChildNode);
//            this.openF.updateF(dupChildNode, dupF);
                            }
                            // Otherwise, consider to reopen dupChildNode
                            else {
                                // Return to OPEN list only if reopening is allowed
                                if (this.reopen) {
                                    ++result.reopened;
                                    _addNode(dupChildNode);
                                }
                            }
                            // in any case, update closed to be bestChild
                            this.closed.put(dupChildNode.packed, dupChildNode);

                        } else {
                            // A shorter path has not been found, but let's update the node in open if its h increased
                            if (this.useBPMX) {
                                if (dupChildNode.getIndex(this.open.getKey()) != -1) {
                                    this.open.update(dupChildNode);
                                }
                            }
                        }
                        // Otherwise, the node is new (hasn't been reached yet)
                    } else {
                        // Propagate the H value to child (in case of BPMX)
                        if (this.useBPMX) {
                            childNode.h = Math.max(childNode.h, currentNode.h - edgeCost);
                        }
                        _addNode(childNode);
                    }
                }
            }
        }
        catch(OutOfMemoryError e){
            System.out.println("[INFO] WAstar OutOfMemory :-( "+e);
            System.out.println("[INFO] OutOfMemory WAstar on:"+this.domain.getClass().getSimpleName()+" generated:"+result.getGenerated());
        }

        result.stopTimer();
//        System.out.println("Generated:\t"+result.getGenerated());
//        System.out.println("closed Size:\t"+this.closed.size());
//        result.printArrCpuTimeMillis();



        // If a goal was found: update the solution
        if (goal != null) {
            System.out.print("\r");
            SolutionImpl solution = new SolutionImpl(this.domain);
            List<Operator> path = new ArrayList<>();
            List<State> statesPath = new ArrayList<>();
            // System.out.println("[INFO] Solved - Generating output path.");
            double cost = 0;

            State currentPacked = domain.unpack(goal.packed);
            State currentParentPacked = null;
            for (Node currentNode = goal;
                 currentNode != null;
                 currentNode = currentNode.parent, currentPacked = currentParentPacked) {
                // If op of current node is not null that means that p has a parent
//                System.out.println(currentPacked.dumpStateShort());
                if (currentNode.op != null) {
                    path.add(currentNode.op);
                    currentParentPacked = domain.unpack(currentNode.parent.packed);
                    cost += currentNode.op.getCost(currentPacked, currentParentPacked);
                }
                statesPath.add(domain.unpack(currentNode.packed));
            }
            // The actual size of the found path can be only lower the G value of the found goal
            assert cost <= goal.g;
            double roundedCost = new BigDecimal(cost).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
            double roundedG = new BigDecimal(goal.g).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
            if (roundedCost - roundedG < 0) {
                System.out.println("[INFO] Goal G is higher that the actual cost " +
                        "(G: " + goal.g +  ", Actual: " + cost + ")");
            }

            Collections.reverse(path);
            solution.addOperators(path);

            Collections.reverse(statesPath);
            solution.addStates(statesPath);

            solution.setCost(cost);
            result.addSolution(solution);
        }

        return result;
    }

    /**
     *
     * @return chosen Node for expansion
     */
    protected Node _selectNode() {
        Node toReturn;
        toReturn = this.open.peek();
//        if(openF.getFminCount() < result.generated){
//            toReturn = this.openF.peek();
//        }
        open.remove(toReturn);
//        openF.remove(toReturn);
        return toReturn;
    }

    /**
     *
     * @param toAdd is the new node toAdd to open
     */
    protected void _addNode(Node toAdd) {
        this.open.add(toAdd);
//        this.openF.add(toAdd);
        // The nodes are ordered in the closed list by their packed values
        this.closed.put(toAdd.packed, toAdd);
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return WAStar.WAStarPossibleParameters;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            case "weight": {
                this.weight = Double.parseDouble(value);
                if (this.weight < 1.0d) {
                    System.out.println("[ERROR] The weight must be >= 1.0");
                    throw new IllegalArgumentException();
                } else if (this.weight == 1.0d) {
                    System.out.println("[WARNING] Weight of 1.0 is equivalent to A*");
                }
                break;
            }
            case "reopen": {
                this.reopen = Boolean.parseBoolean(value);
                break;
            }
            case "bpmx": {
                this.useBPMX = Boolean.parseBoolean(value);
                if (this.useBPMX) {
                    System.out.println("[INFO] WAStar will be ran with BPMX");
                }
                break;
            }
            case "max-cost": {
                this.maxCost = Double.parseDouble(value);
                if (this.maxCost <= 0) {
                    System.out.println("[ERROR] The maximum possible cost must be >= 0");
                    throw new IllegalArgumentException();
                }
                break;
            }
            case "FR": {
                this.FR = Integer.parseInt(value);
                break;
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * The node class
     */
    protected final class Node extends SearchQueueElementImpl{
    	protected double g;
        protected double h;

        protected Operator op;
        protected Operator pop;

        protected Node parent;
        protected PackedElement packed;
//        private int[] secondaryIndex;

        protected Node(State state, Node parent, State parentState, Operator op, Operator pop) {
            // Size of key
            super(2);
            // TODO: Why?
//            this.secondaryIndex = new int[(heapType == HeapType.BUCKET) ? 2 : 1];
            double cost = (op != null) ? op.getCost(state, parentState) : 0;
            this.h = state.getH();
            // If each operation costs something, we should add the cost to the g value of the parent
            this.g = (parent != null) ? parent.g + cost : cost;

            // Parent node
            this.parent = parent;
            this.packed = WAStar.this.domain.pack(state);
            this.pop = pop;
            this.op = op;
        }

        /**
         * @return The value of the weighted evaluation function
         */
        public double getWf() {
            return this.g + (WAStar.this.weight * this.h);
        }

        /**
         * @return The value of the regular evaluation function
         */
        public double getRf() {
            return this.g + this.h;
        }

        /**
         * A constructor of the class that instantiates only the state
         *
         * @param state The state which this node represents
         */
        protected Node(State state) {
            this(state, null, null, null, null);
        }

        @Override
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
        public double getD() {
            return 0;
        }

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
    }

    /**
     * The nodes comparator class
     */
    protected final class NodeComparator implements Comparator<Node> {

        @Override
        public int compare(final Node a, final Node b) {
            // First compare by wF (smaller is preferred), then by g (bigger is preferred)
            if (a.getWf() < b.getWf()) return -1;
            if (a.getWf() > b.getWf()) return 1;
            if (a.g > b.g) return -1;
            if (a.g < b.g) return 1;
            return 0;
        }
    }

}
