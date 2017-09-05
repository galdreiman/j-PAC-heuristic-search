package org.cs4j.core.pac.conf;

/**
 * Created by user on 19/08/2017.
 */
public class MLPacPreprocessExperimentValues {

    private Class domainClass;
    private int trainLevelLow;
    private int trainLevelHigh;
    private int trainLevelDelta;
    private int testLevel;

    @Override
    public String toString() {
        return "MLPacPreprocessExperimentValues{" +
                "domainClass=" + domainClass +
                ", trainLevelLow=" + trainLevelLow +
                ", trainLevelHigh=" + trainLevelHigh +
                ", trainLevelDelta=" + trainLevelDelta +
                ", testLevel=" + testLevel +
                '}';
    }

    public MLPacPreprocessExperimentValues(Class domainClass, int trainLevelLow, int trainLevelHigh, int trainLevelDelta, int testLevel) {
        this.domainClass = domainClass;
        this.trainLevelLow = trainLevelLow;
        this.trainLevelHigh = trainLevelHigh;
        this.trainLevelDelta = trainLevelDelta;
        this.testLevel = testLevel;
    }

    public void setDomainClass(Class domainClass) {
        this.domainClass = domainClass;
    }

    public void setTrainLevelLow(int trainLevelLow) {
        this.trainLevelLow = trainLevelLow;
    }

    public void setTrainLevelHigh(int trainLevelHigh) {
        this.trainLevelHigh = trainLevelHigh;
    }

    public void setTrainLevelDelta(int trainLevelDelta) {
        this.trainLevelDelta = trainLevelDelta;
    }

    public void setTestLevel(int testLevel) { this.testLevel = testLevel; }

    public Class getDomainClass() {
        return domainClass;
    }

    public int getTrainLevelLow() {
        return trainLevelLow;
    }

    public int getTrainLevelHigh() {
        return trainLevelHigh;
    }

    public int getTrainLevelDelta() {
        return trainLevelDelta;
    }

    public int getTestLevel() { return  this.testLevel;}
}
