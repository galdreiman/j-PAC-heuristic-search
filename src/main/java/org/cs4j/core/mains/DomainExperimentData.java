package org.cs4j.core.mains;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.domains.*;
import org.cs4j.core.pac.conf.PacConfig;

import java.io.File;
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
		TRAIN, TEST, ALL, DEBUG
	}

	private static final double TRAIN_PRESENTAGE = PacConfig.instance.trainRatio();
	private static final int NUM_OF_INSTANCES = 10;//PacConfig.instance.numInstances();

	private static Map<Class<? extends SearchDomain>, DomainExperimentData> domainToExperimentDataTrain;
	private static Map<Class<? extends SearchDomain>, DomainExperimentData> domainToExperimentDataTest;
	private static Map<Class<? extends SearchDomain>, DomainExperimentData> domainToExperimentDataAll;

	// This defined experiments data to be used for debugging purposes. It can
	// be set from outside this class
	private static Map<Class<? extends SearchDomain>, DomainExperimentData> domainToExperimentDataDebugRange;

	static {
		// All instances set configuration
		domainToExperimentDataAll = new HashMap<>();



		domainToExperimentDataAll.put(FifteenPuzzle.class,
				new DomainExperimentData("./input/fifteenpuzzle/states15",
						"./input/fifteenpuzzle/states%d",
				"."+File.separator+"preprocessResults"+File.separator+"FifteenPuzzle"+File.separator,
				"."+File.separator+"results"+File.separator+"FifteenPuzzle"+File.separator,
				1, NUM_OF_INSTANCES,
				"",
				""));
		domainToExperimentDataAll.put(Pancakes.class,
				new DomainExperimentData("."+File.separator+"input"+File.separator+"pancakes"+File.separator+"generated-40",
						"."+File.separator+"input"+File.separator+"pancakes"+File.separator+"generated-%d",
						"."+File.separator+"preprocessResults"+File.separator+"pancakes"+File.separator,
						"."+File.separator+"results"+File.separator+"pancakes"+File.separator,
						1, NUM_OF_INSTANCES,
						"."+File.separator+"input"+File.separator+"pancakes"+File.separator+"generated-for-pac-stats-%d",
						"."+File.separator+"preprocessResults"+File.separator+"pancakes"+File.separator+"%d"));
		domainToExperimentDataAll.put(GridPathFinding.class,
				new DomainExperimentData(
				"."+File.separator+"input"+File.separator+"gridpathfinding"+File.separator+"brc202d.map",
						"."+File.separator+"input"+File.separator+"gridpathfinding"+File.separator+"brc202d.map",
				"."+ File.separator+"preprocessResults"+File.separator+"gridpathfinding"+File.separator,
				"."+File.separator+"results"+File.separator+"gridpathfinding"+File.separator,
				1, NUM_OF_INSTANCES,
				"",
				""));
		domainToExperimentDataAll.put(VacuumRobot.class,
				new DomainExperimentData(
				"."+File.separator+"input"+File.separator+"vacuumrobot"+File.separator+"generated-10-dirt",
						"."+File.separator+"input"+File.separator+"vacuumrobot"+File.separator+"generated-%d-dirt",
						"."+File.separator+"preprocessResults"+File.separator+"VacuumRobot"+File.separator,
						"."+File.separator+"results"+File.separator+"VacuumRobot"+File.separator,
						1, NUM_OF_INSTANCES,
						"."+File.separator+"input"+File.separator+"vacuumrobot"+File.separator+"generated-%d-dirt",
						""));
		domainToExperimentDataAll.put(DockyardRobot.class,
				new DomainExperimentData("."+File.separator+"input"+File.separator+"dockyard-robot-max-edge-2-out-of-place-30",
						"."+File.separator+"input"+File.separator+"dockyard-robot-max-edge-2-out-of-place-%d",
						"."+File.separator+"preprocessResults"+File.separator+"dockyard-robot-max-edge-2-out-of-place-30"+File.separator,
						"."+File.separator+"results"+File.separator+"dockyard-robot-max-edge-2-out-of-place-30"+File.separator,
						1,
						90,
						"",
						""));

		// Training set configuration
		int trainEndIndex = (int) (NUM_OF_INSTANCES * TRAIN_PRESENTAGE);
		int testStartIndex = trainEndIndex + 1;

		domainToExperimentDataTrain = new HashMap<>();
		domainToExperimentDataTest = new HashMap<>();
		domainToExperimentDataDebugRange = new HashMap<>();

		for (Class domainClass : domainToExperimentDataAll.keySet()) {
			domainToExperimentDataTrain.put(domainClass,
					domainToExperimentDataAll.get(domainClass).subset(1, trainEndIndex));
			domainToExperimentDataTest.put(domainClass, domainToExperimentDataAll.get(domainClass)
					.subset(testStartIndex, domainToExperimentDataAll.get(domainClass).toInstance));
			domainToExperimentDataDebugRange.put(domainClass, domainToExperimentDataAll.get(domainClass).subset(1, 10));
		}
	}

	public static void setDebugRnage(int fromInstance, int toInstance) {
		domainToExperimentDataDebugRange.get(RunType.DEBUG).fromInstance = fromInstance;
		domainToExperimentDataDebugRange.get(RunType.DEBUG).toInstance = toInstance;
	}

	/**
	 * Get the relevant DomainExperimentData for this class of domain and run
	 * configuration (train,test,all)
	 */
	public static DomainExperimentData get(Class<? extends SearchDomain> domainClass, RunType runType) {
		if (runType.equals(RunType.TRAIN))
			return domainToExperimentDataTrain.get(domainClass);
		else if (runType.equals(RunType.TEST)) {
			return domainToExperimentDataTest.get(domainClass);
		} else if (runType.equals(runType.ALL))
			return domainToExperimentDataAll.get(domainClass);
		throw new IllegalArgumentException("Trying to get unknown DomainExperimentData");
	}

	public String inputPath; // The directory where the problem instances are
	public String inputPathFormat; // The directory where the problem instances are
	public String outputPreprocessPath; // The directory where to output the experimental
								// results
	public String outputOnlinePath; // The directory where to output the experimental
							// results
	public int fromInstance; // The problem instance to start from
	public int toInstance; // // The problem instance to finish at (inclusive)
	public String pacInputPathFormat;
	public String outputPreprocessPathFormat;

//	public DomainExperimentData(String inputPath, String outputPreprocessPath, String outputOnlinePath, int fromInstance, int toInstance) {
//		this(inputPath,outputPreprocessPath,outputOnlinePath,fromInstance,toInstance,"");
//	}

	public DomainExperimentData(String inputPath, String inputPathFormat, String outputPreprocessPath, String outputOnlinePath, int fromInstance,
								int toInstance, String pacInputPathFormat,String outputPreprocessPathFormat) {
		this.inputPath = inputPath;
		this.inputPathFormat = inputPathFormat;
		this.outputPreprocessPath = outputPreprocessPath;
		this.outputOnlinePath = outputOnlinePath;
		this.fromInstance = fromInstance;
		this.toInstance = toInstance;
		this.pacInputPathFormat = pacInputPathFormat;
		this.outputPreprocessPathFormat = outputPreprocessPathFormat;
	}

	/**
	 * Returns a new DomainExperimentData object that consists of a subset of
	 * the instances of this object.
	 * 
	 * @param fromInstance
	 *            from instance
	 * @param toInstance
	 *            to instance
	 */
	public DomainExperimentData subset(int fromInstance, int toInstance) {
		return new DomainExperimentData(this.inputPath,this.inputPathFormat, this.outputPreprocessPath, this.outputOnlinePath, fromInstance, toInstance,"","");
	}

}
