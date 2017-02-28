package org.cs4j.core.algorithms;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.collections.BucketHeap;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.collections.SearchQueueElement;

/**
 * The Node is the basic data structure which is used by the algorithm during the search -
 * OPEN and CLOSED lists contain nodes which are created from the domain states
 */
public class AnytimeSearchNode extends SearchQueueElementImpl implements BucketHeap.BucketHeapElement {
    public double g;
    public double h;
    public double d;
    public SearchDomain.Operator op;
    public SearchDomain.Operator pop;
    public AnytimeSearchNode parent;
    public PackedElement packed;
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
    public AnytimeSearchNode(SearchDomain domain,
                             SearchDomain.State state,
                             AnytimeSearchNode parent,
                             SearchDomain.State parentState,
                             SearchDomain.Operator op,
                             SearchDomain.Operator pop) {
        // The size of the key (for SearchQueueElementImpl) is 1
        super(1);
        this.secondaryIndex = new int[1];
        this.h = state.getH();
        this.d = state.getD();
        double cost = (op != null) ? op.getCost(state, parentState) : 0;
        this.g = (parent != null)? parent.g + cost : cost;
        this.parent = parent;
        this.packed = domain.pack(state);
        this.pop = pop;
        this.op = op;
    }

    public AnytimeSearchNode(SearchDomain domain,
                             SearchDomain.State state){
        this(domain, state, null, null,null,null);
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
