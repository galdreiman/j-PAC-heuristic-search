package org.cs4j.core.mains;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.domains.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roni Stern on 24/02/2017.
 *
 * A helper class to hold information needed to run experiments on a given
 * domain
 */
public class DomainExperimentData {
	public enum RunType {
		TRAIN, TEST, ALL
	}

	private static final double TRAIN_PRESENTAGE = 0.5;
	private static final int NUM_OF_INSTANCES = 100;

	private static Map<Class<? extends SearchDomain>, DomainExperimentData> domainToExperimentDataTrain;
	private static Map<Class<? extends SearchDomain>, DomainExperimentData> domainToExperimentDataTest;
	private static Map<Class<? extends SearchDomain>, DomainExperimentData> domainToExperimentDataAll;
	static {
        // All instances set configuration
        domainToExperimentDataAll = new HashMap<>();
        domainToExperimentDataAll.put(FifteenPuzzle.class, new DomainExperimentData("./input/fifteenpuzzle/states15",
                "./results/FifteenPuzzle/", 1, NUM_OF_INSTANCES));
        domainToExperimentDataAll.put(Pancakes.class, new DomainExperimentData("./input/pancakes/generated-40",
                "./results/pancakes/", 1, NUM_OF_INSTANCES));
        domainToExperimentDataAll.put(GridPathFinding.class,
                new DomainExperimentData("./input/GridPathFinding/brc202d.map", "./results/GridPathFinding/",
                        1, NUM_OF_INSTANCES));
        domainToExperimentDataAll.put(VacuumRobot.class, new DomainExperimentData(
                "./input/vacuumrobot/generated-5-dirt", "./results/vacuumrobot/", 1, NUM_OF_INSTANCES));
        domainToExperimentDataAll.put(DockyardRobot.class,
                new DomainExperimentData("./input/dockyard-robot-max-edge-2-out-of-place-30",
                        "./results/dockyard-robot-max-edge-2-out-of-place-30/", 1, NUM_OF_INSTANCES));


        // Training set configuration
        int trainEndIndex = (int) (NUM_OF_INSTANCES * TRAIN_PRESENTAGE);
        int testStartIndex = trainEndIndex + 1;

		domainToExperimentDataTrain = new HashMap<>();
        domainToExperimentDataTest = new HashMap<>();

        for(Class domainClass : domainToExperimentDataAll.keySet()){
            domainToExperimentDataTrain.put(domainClass,
                    domainToExperimentDataAll.get(domainClass).subset(1, trainEndIndex));
            domainToExperimentDataTest.put(domainClass,
                    domainToExperimentDataAll.get(domainClass).subset(testStartIndex, NUM_OF_INSTANCES));
        }
	}


    /**
     * Get the relevant DomainExperimentData for this class of domain and run confiuratino (train,test,all)
     */
	public static DomainExperimentData get(Class<? extends SearchDomain> domainClass, RunType runType)  {
		if(runType.equals(RunType.TRAIN))
		return domainToExperimentDataTrain.get(domainClass);
		else if(runType.equals(RunType.TEST)){
			return domainToExperimentDataTest.get(domainClass);
		}
		throw new IllegalArgumentException("Trying to get unknown DomainExperimentData");
	}



	public String inputPath; // The directory where the problem instances are
	public String outputPath; // The directory where to output the experimental results
	public int fromInstance; // The problem instance to start from
	public int toInstance; // // The problem instance to finish at (inclusive)

	public DomainExperimentData(String inputPath, String outputPath, int fromInstance, int toInstance) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.fromInstance = fromInstance;
		this.toInstance = toInstance;
	}

    /**
     * Returns a new DomainExperimentData object that consists of a subset of the instances of this object.
     * @param fromInstance from instance
     * @param toInstance to instance
     */
	public DomainExperimentData subset(int fromInstance,int toInstance){
	    return new DomainExperimentData(this.inputPath,
                this.outputPath,
                fromInstance,
                toInstance);
    }

}
