package org.cs4j.core;

/**
 * Created by Roni Stern on 28/02/2017.
 */
abstract public class AbstractSearchDomain implements SearchDomain {
    protected State initialState=null;

    /**
     * Returns the initial state for an instance of Domain.
     *
     *  @return the initial state
     */
    public State initialState(){
        if(this.initialState==null)
            this.initialState=this.createInitialState();
        return this.initialState;
    }

    public void setInitialState(State initialState){
        this.initialState=initialState;
    }


    /**
     * Creates the initial state
     */
    abstract protected State createInitialState();

}
