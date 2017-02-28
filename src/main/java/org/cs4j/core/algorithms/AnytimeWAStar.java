package org.cs4j.core.algorithms;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roni Stern on 23/02/2017.
 */
public class AnytimeWAStar extends AbstractAnytimeSearch{
    private static final Map<String, Class> POSSIBLE_PARAMETERS;
    private double weight;


    // Declare the parameters that can be tunes before running the search
    static
    {
        POSSIBLE_PARAMETERS = new HashMap<>();
        POSSIBLE_PARAMETERS .put("weight", Double.class);
    }

    public AnytimeWAStar() {
        super();
    }


    @Override
    public Map<String, Class> getPossibleParameters() {
        return AnytimeWAStar.POSSIBLE_PARAMETERS;
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
            default: {
                System.err.println("No such parameter: " + parameterName + " (value: " + value + ")");
                throw new UnsupportedOperationException();
            }
        }
    }



    /**
     * Create a node comparator used by the open list to prioritize the nodes
     */
    @Override
    protected Comparator<AnytimeSearchNode> createNodeComparator()
    {
        return new AnytimeWAStar.NodeComparator();
    }

    /**
     * The node comparator class
     */
    private final class NodeComparator implements Comparator<AnytimeSearchNode> {
        @Override
        public int compare(final AnytimeSearchNode a, final AnytimeSearchNode b) {
            double aCost = a.g+AnytimeWAStar.this.weight*a.h;
            double bCost = b.g+AnytimeWAStar.this.weight*b.h;

            if (aCost < bCost) return -1;
            if (aCost > bCost) return 1;
                        // Tie breaking using h
            if(a.h<b.h) return -1;
            if(b.h<a.h) return 1;

            return 0;
        }
    }
}
