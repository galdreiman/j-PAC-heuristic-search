package org.cs4j.core.experiments;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;

import org.apache.log4j.Logger;
import org.cs4j.core.*;
import org.cs4j.core.algorithms.DP;
import org.cs4j.core.algorithms.pac.*;
import org.cs4j.core.domains.*;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

/**
 * This class runs PAC search experiments. It assumes the pre-process is given.
 */
public class PACOnlineExperimentRunner {
	final static Logger logger = Logger.getLogger(PACOnlineExperimentRunner.class);

	/**
	 * Extract from SearchResults the data to output to the file
	 * 
	 * @param resultsData
	 *            the results data object to update with data from the
	 *            SearchResults
	 * @param result
	 *            the SearchResults object to extract from
	 */
	protected void setResultsData(List resultsData, SearchResult result) {

		resultsData.add(result.hasSolution() ? 1 : 0);
		if (result.hasSolution()) {
			resultsData.add(result.getBestSolution().getLength());
			resultsData.add(result.getBestSolution().getCost());
		}else{
			resultsData.add(-1);
			resultsData.add(-1);
		}
		resultsData.add(result.getSolutions().size());
		resultsData.add(result.getGenerated());
		resultsData.add(result.getExpanded());
		resultsData.add(result.getCpuTimeMillis());
		resultsData.add(result.getWallTimeMillis());
	}

	public void run(Class domainClass, SearchAlgorithm alg, String inputPath, OutputResult output, int startInstance,
			int stopInstance, SortedMap<String, String> domainParams, SortedMap<String, Object> runParams) {
		SearchDomain domain;
		SearchResult result;
		List resultsData;

		Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
		try {
			logger.info("Solving " + domainClass.getName() + "\tAlg: " + alg.getName());
			// search on this domain and algo and weight the 100 instances
			for (int i = startInstance; i <= stopInstance; ++i) {
				try {
					// Read domain from file
					logger.info("\rSolving " + domainClass.getName() + "\t instance " + i + "\t Alg: " + alg.getName());
					domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
					result = alg.search(domain);
					logger.info("Solution found? " + result.hasSolution());

					resultsData = new ArrayList<>();
					resultsData.add(i); // Instance ID
					setResultsData(resultsData, result); // Search results data
															// (expanded, cpu
															// time, etc.)
					resultsData.addAll(runParams.values()); // Parameters that
															// are constant for
															// this run (w,
															// domain, etc.)

					output.appendNewResult(resultsData.toArray());
					output.newline();
				} catch (OutOfMemoryError e) {
					logger.info("OutOfMemory in:" + alg.getName() + " on:" + domainClass.getName());
				} catch (FileNotFoundException e) {
					logger.info("FileNotFoundException At inputPath:" + inputPath);
					i = stopInstance; // @TODO: Is this just a replacement for a
										// break?
				}
			}
		} catch (IOException e) {
			logger.error(e);
		}
	}

	/**
	 * Print the headers for the experimental results into the output file
	 * 
	 * @param output
	 * @param runParams
	 * @throws IOException
	 */
	public void printResultsHeaders(OutputResult output, SortedMap<String, Object> runParams) throws IOException {
		String[] defaultColumnNames = new String[] { "InstanceID", "Found", "Depth", "Cost", "Iterations", "Generated",
				"Expanded", "Cpu Time", "Wall Time" };
		List<String> runParamColumns = new ArrayList<>(runParams.keySet());
		List<String> columnNames = new ArrayList();
		for (String columnName : defaultColumnNames)
			columnNames.add(columnName);
		columnNames.addAll(runParamColumns);
		String toPrint = String.join(",", columnNames);
		output.writeln(toPrint);
	}

	/**
	 * An example of using ExperimentRunner
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Class[] domains = { GridPathFinding.class, Pancakes.class, VacuumRobot.class };
		Class[] pacConditions = { TrivialPACCondition.class, RatioBasedPACCondition.class, FMinCondition.class };
		double[] epsilons = { 0, 0.1, 0.25, 0.5, 0.75, 1 };// ,1 ,1.5};
		double[] deltas = { 0, 0.1, 0.25, 0.5, 0.75, 0.8, 1 };
		SortedMap<String, String> domainParams = new TreeMap<>();
		SortedMap<String, Object> runParams = new TreeMap<>();
		OutputResult output = null;

		runParams.put("epsilon", -1);
		runParams.put("delta", -1);
		runParams.put("pacCondition", -1);
		Class anytimeSearchClass = AnytimePTS4PAC.class;

		SearchAlgorithm pacSearch = new PACSearchFramework();
		pacSearch.setAdditionalParameter("anytimeSearch", anytimeSearchClass.getName());
		PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();

		for (Class domainClass : domains) {
			logger.info("Running anytime for domain " + domainClass.getName());
			try {
				// Prepare experiment for a new domain
				output = new OutputResult(DomainExperimentData.get(domainClass,RunType.TEST).outputPath, "PAC", -1, -1, null, false,
						true);
				runParams.put("anytimeSearch", anytimeSearchClass.getSimpleName());
				runner.printResultsHeaders(output, runParams);

				PACUtils.loadPACStatistics(domainClass);
				for (Class pacConditionClass : pacConditions) {
					runParams.put("pacCondition", pacConditionClass.getSimpleName());
					for (double epsilon : epsilons) {
						runParams.put("epsilon", epsilon);
						for (double delta : deltas) {
							runParams.put("delta", delta);
							pacSearch.setAdditionalParameter("epsilon", "" + epsilon);
							pacSearch.setAdditionalParameter("delta", "" + delta);
							pacSearch.setAdditionalParameter("pacCondition", pacConditionClass.getName());
							runner.run(domainClass, pacSearch, DomainExperimentData.get(domainClass,RunType.TEST).inputPath, output,
									DomainExperimentData.get(domainClass,RunType.TEST).fromInstance,
									DomainExperimentData.get(domainClass,RunType.TEST).toInstance, domainParams, runParams);
						}
					}
				}

				// Run DPS on the same epsilon values
				runParams.put("delta", -1);
				SearchAlgorithm dps = new DP("DPS", false, false, false); // A
																			// bounded-suboptimal
																			// algorithm
				runParams.put("searcher", dps.getClass().getSimpleName());
				for (double epsilon : epsilons) {
					runParams.put("epsilon", epsilon);
					dps.setAdditionalParameter("weight", "" + (1 + epsilon));
					runner.run(domainClass, dps, DomainExperimentData.get(domainClass,RunType.TEST).inputPath, output,
							DomainExperimentData.get(domainClass,RunType.TEST).fromInstance,
							DomainExperimentData.get(domainClass,RunType.TEST).toInstance, domainParams, runParams);
				}
			} catch (IOException e) {
				logger.error(e);
			} finally {
				if (output != null)
					output.close();
			}
		}
	}
}
