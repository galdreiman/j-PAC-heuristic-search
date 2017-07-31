package org.cs4j.core.pac.evaluation;

import weka.core.DenseInstance;

/**
 * Created by Gal Dreiman on 31/07/2017.
 */
public class PacDenseInstance extends DenseInstance {

    public PacDenseInstance(int numAttributes){
        super(numAttributes);
    }

    public double[] getAttValues(){
        return this.m_AttValues;
    }


}
