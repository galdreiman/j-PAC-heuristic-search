/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cs4j.core.algorithms;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchDomain.Operator;
import org.cs4j.core.SearchDomain.State;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.SearchResultImpl.SolutionImpl;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Iterative Deepening A* Search
 *
 * @author Matthew Hatem
 */
public class IDAstar implements SearchAlgorithm {
    // The domain for the search
    private SearchDomain domain;

    private SearchResultImpl result;
    private SolutionImpl solution;

    private double weight;
    private double bound;
    private double minNextF;

    /**
     * The default constructor of the class
     */
    public IDAstar() {
  	    this(1.0);
    }

    protected IDAstar(double weight) {
        this.weight = weight;
    }

    @Override
    public String getName() {
        return "idastar";
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return null;
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
            default:{
                throw new NotImplementedException();
            }
        }
    }

    @Override
    public SearchResult search(SearchDomain domain) {
        this.result = new SearchResultImpl();
        this.solution = new SolutionImpl();
        State root = domain.initialState();
        this.result.startTimer();
        this.bound = this.weight * root.getH();
        int i = 0;
        do {
            this.minNextF = -1;
            boolean goalWasFound = this.dfs(domain, root, 0, null);
/*            System.out.println("min next f: " + minNextF ) ;
            System.out.println("next");*/
            this.result.addIteration(i, this.bound, this.result.expanded, this.result.generated);
            this.bound = this.minNextF;
            if (goalWasFound) {
                break;
            }
        } while (true);
        this.result.stopTimer();

        SearchResultImpl.SolutionImpl solution = new SearchResultImpl.SolutionImpl(this.domain);
        List<SearchDomain.Operator> path = this.solution.getOperators();
        List<SearchDomain.State> statesPath = this.solution.getStates();

        path.remove(0);
        Collections.reverse(path);
        solution.addOperators(path);

        statesPath.remove(0);
        Collections.reverse(statesPath);
        solution.addStates(statesPath);

        solution.setCost(this.solution.getCost());
        result.addSolution(solution);

        return this.result;
    }

    /**
     * A single iteration of the IDA*
     *
     * @param domain The domain on which the search is performed
     * @param parent The parent state
     * @param cost The cost to reach the parent state
     * @param pop The reverse operator?
     *
     * @return Whether a solution was found
     */
    private boolean dfs(SearchDomain domain, State parent, double cost, Operator pop) {
        double f = cost + this.weight * parent.getH();
    
        if (f <= this.bound && domain.isGoal(parent)) {
            this.solution.setCost(f);
            this.solution.addOperator(pop);
            return true;
        }

        if (f > this.bound) {
            // Let's record the lowest value of f that is greater than the bound
            if (this.minNextF < 0 || f < this.minNextF)
                this.minNextF = f;
            return false;
        }

        // Expand the current node
        ++result.expanded;
        int numOps = domain.getNumOperators(parent);
        for (int i = 0; i < numOps; ++i) {
    	    Operator op = domain.getOperator(parent, i);
            // Bypass reverse operators
            if (op.equals(pop)) {
                continue;
            }
            ++result.generated;
            State child = domain.applyOperator(parent, op);
            boolean goal = this.dfs(domain, child, op.getCost(child, parent) + cost, op.reverse(parent));
            if (goal) {
                this.solution.addOperator(op);
                this.solution.addState(parent);
                return true;
            }
        }

        // No solution was found
        return false;
    }
}
