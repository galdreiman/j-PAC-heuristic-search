package org.cs4j.core.experiments;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;

import com.sun.java.browser.dom.DOMAccessException;
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

	public void run(Experiment experiment,
					Class domainClass,
					RunType runType,
					OutputResult output,
					SortedMap<String, String> domainParams,
					SortedMap<String, Object> runParams){
		SearchDomain domain;

		Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
		logger.info("Solving " + domainClass.getName());
		int fromInstance = DomainExperimentData.get(domainClass,runType).fromInstance;
		int toInstance = DomainExperimentData.get(domainClass,runType).toInstance;
		String inputPath = DomainExperimentData.get(domainClass,runType).inputPath;

		// search on this domain and algo and weight the 100 instances
		for (int i = fromInstance; i <= toInstance; ++i) {
			logger.info("\rSolving " + domainClass.getName() + "\t instance " + i + "\t"+runParamsToLog(runParams));
			domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
			experiment.run(domain, output, i, runParams);
		}
	}

	private String runParamsToLog(SortedMap<String, Object> runParams){
		StringBuilder builder = new StringBuilder();
		for(String param : runParams.keySet()){
			builder.append(param);
			builder.append("\t");
			builder.append(runParams.get(param));
		}
		return builder.toString();
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
	 * Run a batch of experiments
	 * @param domains A set of domain classes
	 * @param pacConditions a set of PAC condition classes
	 * @param epsilons possible epsilon values
	 * @param deltas possible delta values
	 * @param experiment an experiment runner object
	 */
	public void runExperimentBatch(Class[] domains,
									Class[] pacConditions,
									double[] epsilons,
									double[] deltas,
									Experiment experiment) {
		SortedMap<String, String> domainParams = new TreeMap<>();
		SortedMap<String, Object> runParams = new TreeMap<>();
		OutputResult output = null;
		runParams.put("epsilon", -1);
		runParams.put("delta", -1);
		runParams.put("pacCondition", -1);

		for (Class domainClass : domains) {
			logger.info("Running anytime for domain " + domainClass.getName());
			try {
				// Prepare experiment for a new domain
				output = new OutputResult(DomainExperimentData.get(domainClass, RunType.TEST).outputPath, "PAC", -1, -1, null, false,
						true);
				this.printResultsHeaders(output, runParams);

				PACUtils.getPACStatistics(domainClass); // Loads the statistics from the disk
				for (Class pacConditionClass : pacConditions) {
					runParams.put("pacCondition", pacConditionClass.getSimpleName());
					for (double epsilon : epsilons) {
						runParams.put("epsilon", epsilon);
						for (double delta : deltas) {
							runParams.put("delta", delta);
							runParams.put("pacCondition", pacConditionClass.getName());
							this.run(experiment,domainClass, RunType.TEST, output,
									domainParams, runParams);
						}
					}
				}
			} catch (IOException e) {
				logger.error(e);
			} finally {
				if (output != null)
					output.close();
			}
		}
	}

	/**
	 * Running PAC experiments
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Class[] domains = { Pancakes.class, GridPathFinding.class, VacuumRobot.class, DockyardRobot.class,FifteenPuzzle.class };
		Class[] pacConditions = { TrivialPACCondition.class, RatioBasedPACCondition.class, FMinCondition.class };
		double[] epsilons = { 0, 0.1, 0.25, 0.5, 0.75, 1 };// ,1 ,1.5};
		double[] deltas = { 0, 0.1, 0.25, 0.5, 0.75, 0.8, 1 };
		Experiment experiment = new StandardExperiment(new PACSearchFramework());
		PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
		runner.runExperimentBatch(
				domains,
				pacConditions,
				epsilons,
				deltas,
				experiment);
	}

}
