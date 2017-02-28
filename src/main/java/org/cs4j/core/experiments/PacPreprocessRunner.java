package org.cs4j.core.experiments;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.cs4j.core.AnytimeSearchAlgorithm;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.domains.*;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

/**
 * This class is designed to collect statistics used for the PAC search research
 */
public class PacPreprocessRunner {

	final static Logger logger = Logger.getLogger(PacPreprocessRunner.class);

	public double[] run(Class domainClass, String inputPath, String outputPath, int startInstance,
			int stopInstance, HashMap<String, String> domainParams) {

		double[] resultsData;
		SearchDomain domain;
		SearchResult result;
		OutputResult output = null;

		// Construct a variant of A* that records also the h value of the start state
		WAStar astar = new WAStar() {
			@Override
			public SearchResult search(SearchDomain domain) {
				double initialH = domain.initialState().getH();
				SearchResult results = super.search(domain);
				results.getExtras().put("initial-h",initialH);
				return results;
			}};
		astar.setAdditionalParameter("weight","1.0");


		logger.info("init experiment");
		try {
			// Print the output headers
			output = new OutputResult(outputPath, "Preprocess_", -1, -1, null, false,true);
			String[] resultColumnNames = { "InstanceID", "h*(s)", "h(s)", "h*/h" };
			String toPrint = String.join(",", resultColumnNames);
			output.writeln(toPrint);

			// Get the domains constructor
			Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
			logger.info("Start running search for " + (stopInstance - startInstance + 1) +" instances:");
			for (int i = startInstance; i <= stopInstance; ++i) {
				try {
					logger.info("Running the " + i +"'th instance");
					// Read domain from file
					resultsData = new double[resultColumnNames.length];
					domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
					result = astar.search(domain);
					logger.info("Solution found? " + result.hasSolution());

					setResultsData(result, resultsData, i);
					output.appendNewResult(resultsData);
					output.newline();

				} catch (OutOfMemoryError e) {
					logger.error("PacPreprocessRunner OutOfMemory :-( ", e);
					logger.error("OutOfMemory in:" + astar.getName() + " on:" + domainClass.getName());
				}
			}
		} catch (IOException e1) {
		} finally {
			output.close();
		}
		return null;
	}




	private void setResultsData(SearchResult result, double[] resultsData, int i) {
		int instanceId = i;
		double cost=-1;
		// RONI: I hate these unreadable one-liners
		if(result.hasSolution()){
			cost = result.getSolutions().get(0).getCost();
		}
		double initialH = (double)result.getExtras().get("initial-h");
		double suboptimality = cost / initialH;
		
		resultsData[0] = instanceId;
		resultsData[1] = cost;
		resultsData[2] = initialH;
		resultsData[3] = suboptimality;
	}

	/**
	 * Run the PAC preprocess on all domains
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		PacPreprocessRunner runner = new PacPreprocessRunner();
		HashMap domainParams = new HashMap<>();
		Class[] domains = {
				GridPathFinding.class,
				FifteenPuzzle.class,
				Pancakes.class,
				VacuumRobot.class,
				DockyardRobot.class};

		for(Class domainClass : domains) {
			logger.info("Running PacPreprocessRunner on domain "+domainClass.getSimpleName());
			runner.run(domainClass,
					DomainExperimentData.get(domainClass,RunType.TRAIN).inputPath,
					DomainExperimentData.get(domainClass,RunType.TRAIN).outputPath,
					DomainExperimentData.get(domainClass,RunType.TRAIN).fromInstance,
					DomainExperimentData.get(domainClass,RunType.TRAIN).toInstance,
					domainParams);
		}
	}

}
