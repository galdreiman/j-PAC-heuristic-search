package org.cs4j.core.algorithms.pac.preprocess;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.cs4j.core.AnytimeSearchAlgorithm;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.AnytimePTSForMLPac;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.domains.VacuumRobot;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

public class MLPacPreprocess {

	private final static Logger logger = Logger.getLogger(MLPacPreprocess.class);
	private static OutputResult output;

	/**
	 * for each domain in Domains: 1. train a classifier: 1.1 for each problem
	 * in domain: apply AnytimeSearchAlgo on problem: for each solution extract
	 * features 1.2 train the model with features table
	 * 
	 * @param args
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) {
		Class[] domains = { VacuumRobot.class }; // , DockyardRobot.class,
													// FifteenPuzzle.class };

		double inputEpsilon = 0.2; // TODO: get input from user (from console,
									// e.g. args[] or whatever)

		for (Class domainClass : domains) {
			logger.info("Running anytime for domain " + domainClass.getName());
			try {
				logger.info("Solving " + domainClass.getName());

				// -------------------------------------------------
				// 1. load PAC statistics (to get optimal solutions
				PACUtils.getPACStatistics(domainClass);

				// --------------------------------------------------------------
				// 2. run anytime search and for every solution collect features
				SearchDomain domain;
				Map<String, String> domainParams = new TreeMap<>();
				Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
				int fromInstance = DomainExperimentData.get(domainClass, RunType.TRAIN).fromInstance;
				int toInstance = DomainExperimentData.get(domainClass, RunType.TRAIN).toInstance;
				String inputPath = DomainExperimentData.get(domainClass, RunType.TRAIN).inputPath;

				AnytimeSearchAlgorithm algorithm = getAnytimeAlg();
				List<SearchResultImpl> searchResultsList = new ArrayList<>();
				output = new OutputResult(
						DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TRAIN).outputPath,
						"ML_PAC_ConditionPreprocessFeatures", -1, -1, null, false, true);
				output.writeln(getHeaderLineFeatures());

				for (int i = fromInstance; i <= toInstance; ++i) {
					logger.info("\rSolving " + domainClass.getName() + "\t instance " + i);
					domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
					double optimalCost = PACUtils.getOptimalSolution(domainClass, i);

					int problemAttemptIndx = 1;
					SearchResultImpl result = (SearchResultImpl) algorithm.search(domain);
					searchResultsList.add(result);

					// if another solution is possible - continue searing
					while (result.hasSolution()) {
						// extract feature out of every solution:
						ExtractFeaturesToFile(result, domain, domainClass,problemAttemptIndx++, i, optimalCost, inputEpsilon);
						
						// continue to search another solution:
						result = (SearchResultImpl) algorithm.continueSearch();
						searchResultsList.add(result);
					}

					System.out.println("------------------");
					System.out.println(searchResultsList.size());
					System.out.println("------------------");

				}

			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			} finally {
				if (output != null) {
					output.close();
				}
			}
		}
	}

	private static String getHeaderLineFeatures() {
		String[] attributes = { "domain", "instance", "index", "generated", "expanded", "reopened", "cost", "g1",
				"h1","g2", "h2", "g3","h3", "is-W-opt" };
		return String.join(",", attributes);
	}

	private static void ExtractFeaturesToFile(SearchResultImpl searchResult, SearchDomain domain, Class domainClass,
			int attemptCounter, int instance, double optimalCost, double inputEpsilon) {

		String domainName = domainClass.getSimpleName();
		int problemInstance = instance;
		int attempt = attemptCounter++;

		long generated = searchResult.getGenerated();
		long expanded = searchResult.getExpanded();
		long reopened = searchResult.getReopened();
		double U = searchResult.getBestSolution().getCost();
		int g = searchResult.getBestSolution().getLength();
		double initialH = domain.initialState().getH();
		
		double g1 = (double) searchResult.getExtras().get("g_1");
		double h1 = (double) searchResult.getExtras().get("h_1");
		
		double g2 = (double) searchResult.getExtras().get("g_2");
		double h2 = (double) searchResult.getExtras().get("h_2");
		

		boolean isWOptimal = isWOpttimal(U, g, optimalCost, inputEpsilon);

		String[] lineParts = { domainName, problemInstance + "", attempt + "", generated + "", expanded + "",
				reopened + "", U + "", g + "", initialH + "", g1+ "",h1+ "",g2+ "",h2+ "", isWOptimal + "" };
		String line = String.join(",", lineParts);
		logger.debug("adding new features to table: " + line);
		try {
			output.writeln(line);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	

	private static boolean isWOpttimal(double U, int g, double optimalCost, double inputEpsilon) {
		// a solution consider as W-optimal iff:
		// g(n) + h*(n)(1+epsilon) < U

		return g + (optimalCost * inputEpsilon) < U;
	}

	private static AnytimeSearchAlgorithm getAnytimeAlg() {
		AnytimeSearchAlgorithm algorithm = new AnytimePTSForMLPac() {
			@Override
			public SearchResult search(SearchDomain domain) {
				double initialH = domain.initialState().getH();
				SearchResult results = super.search(domain);
				results.getExtras().put("initial-h", initialH);
				return results;
			}
		};
		return algorithm;
	}
}
