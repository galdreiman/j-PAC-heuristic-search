package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.IDAstar;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.domains.*;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;

/**
 * This class is designed to collect statistics used for the PAC search research
 */
public class PacPreprocessRunner {

	final static Logger logger = Logger.getLogger(PacPreprocessRunner.class);

	public double[] run(Class domainClass, HashMap<String, String> domainParams) {
		String inputPath = DomainExperimentData.get(domainClass, RunType.TRAIN).inputPath;
		String outputPath = DomainExperimentData.get(domainClass, RunType.TRAIN).outputPreprocessPath;
		int fromInstance = DomainExperimentData.get(domainClass, RunType.TRAIN).fromInstance;
		int toInstance = DomainExperimentData.get(domainClass, RunType.TRAIN).toInstance;
		return run(domainClass,inputPath,outputPath,fromInstance,toInstance,domainParams);
	}

	public double[] run(Class domainClass, String inputPath, String outputPath, int startInstance,
			int stopInstance, HashMap<String, String> domainParams) {

		double[] resultsData;
		SearchDomain domain;
		SearchResult result;
		SearchResult idaResult;
		OutputResult output = null;
		IDAstar idaStar;

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
					if(result.hasSolution()==false) {
						idaStar = new IDAstar();
						logger.info("A* failed on instance " + i + ", running IDA*");
						idaResult = idaStar.search(domain);
						if (idaResult.hasSolution() == false) {
							logger.info("IDA* also failed :(");
						}
						idaResult.getExtras().put("initial-h",result.getExtras().get("initial-h"));
						result=idaResult;
					}
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
		Class[] domains = ExperimentUtils.readClasses(args);
		// Default classes
		if(domains.length==0){
			domains = new Class[]{GridPathFinding.class,
				FifteenPuzzle.class,
				Pancakes.class,
				VacuumRobot.class,
				DockyardRobot.class};
		}

		for(Class domainClass : domains) {
			logger.info("Running PacPreprocessRunner on domain "+domainClass.getSimpleName());
			runner.run(domainClass,
					DomainExperimentData.get(domainClass,RunType.ALL).inputPath,
					DomainExperimentData.get(domainClass,RunType.ALL).outputPreprocessPath,
					DomainExperimentData.get(domainClass,RunType.ALL).fromInstance,
					DomainExperimentData.get(domainClass,RunType.ALL).toInstance,
					domainParams);
		}
	}

}
