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
	public static enum RunType {
		TRAIN, TEST;
	}

	private static final double TRAIN_PRESENTAGE = 0.8;
	private static final int NUM_OF_INSTANCES = 100;

	private static final int TRAIN_INSTANCES = (int) (NUM_OF_INSTANCES * TRAIN_PRESENTAGE);
	private static final int TEST_START_INDEX = TRAIN_INSTANCES + 1;

	public static Map<Class<? extends SearchDomain>, DomainExperimentData> domainToExperimentDataTrain;
	public static Map<Class<? extends SearchDomain>, DomainExperimentData> domainToExperimentDataTest;
	static {
		domainToExperimentDataTrain = new HashMap<Class<? extends SearchDomain>, DomainExperimentData>();

		domainToExperimentDataTrain.put(FifteenPuzzle.class, new DomainExperimentData("./input/FifteenPuzzle/states15",
				"./results/FifteenPuzzle/", 1, TRAIN_INSTANCES));
		domainToExperimentDataTrain.put(Pancakes.class,
				new DomainExperimentData("./input/pancakes/generated-40", "./results/pancakes/", 1, TRAIN_INSTANCES));
		domainToExperimentDataTrain.put(GridPathFinding.class, new DomainExperimentData(
				"./input/GridPathFinding/brc202d.map", "./results/GridPathFinding/", 1, TRAIN_INSTANCES));
		domainToExperimentDataTrain.put(VacuumRobot.class, new DomainExperimentData(
				"./input/vacuumrobot/generated-5-dirt", "./results/VacuumRobot/", 1, TRAIN_INSTANCES));
		domainToExperimentDataTrain.put(DockyardRobot.class,
				new DomainExperimentData("./input/dockyard-robot-max-edge-2-out-of-place-30",
						"./results/dockyard-robot-max-edge-2-out-of-place-30/", 1, TRAIN_INSTANCES));

		domainToExperimentDataTest = new HashMap<Class<? extends SearchDomain>, DomainExperimentData>();
		domainToExperimentDataTest.put(FifteenPuzzle.class, new DomainExperimentData("./input/FifteenPuzzle/states15",
				"./results/FifteenPuzzle/", TEST_START_INDEX, NUM_OF_INSTANCES));
		domainToExperimentDataTest.put(Pancakes.class, new DomainExperimentData("./input/pancakes/generated-40",
				"./results/pancakes/", TEST_START_INDEX, NUM_OF_INSTANCES));
		domainToExperimentDataTest.put(GridPathFinding.class,
				new DomainExperimentData("./input/GridPathFinding/brc202d.map", "./results/GridPathFinding/",
						TEST_START_INDEX, NUM_OF_INSTANCES));
		domainToExperimentDataTest.put(VacuumRobot.class, new DomainExperimentData(
				"./input/vacuumrobot/generated-5-dirt", "./results/VacuumRobot/", TEST_START_INDEX, NUM_OF_INSTANCES));
		domainToExperimentDataTest.put(DockyardRobot.class,
				new DomainExperimentData("./input/dockyard-robot-max-edge-2-out-of-place-30",
						"./results/dockyard-robot-max-edge-2-out-of-place-30/", TEST_START_INDEX, NUM_OF_INSTANCES));

	}

	public static DomainExperimentData get(Class<? extends SearchDomain> domainClass, RunType runType)  {
		if(runType.equals(RunType.TRAIN))
		return domainToExperimentDataTrain.get(domainClass);
		else if(runType.equals(RunType.TEST)){
			return domainToExperimentDataTest.get(domainClass);
		}
		throw new IllegalArgumentException("Trying to get unknown DomainExperimentData");
	}

	public String inputPath;
	public String outputPath;
	public int fromInstance;
	public int toInstance;

	public DomainExperimentData(String inputPath, String outputPath, int fromInstance, int toInstance) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.fromInstance = fromInstance;
		this.toInstance = toInstance;
	}
}
