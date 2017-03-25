package org.cs4j.core.algorithms.pac.preprocess;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.cs4j.core.AnytimeSearchAlgorithm;
import org.cs4j.core.MLPacFeatureExtractor;
import org.cs4j.core.MLPacFeatureExtractor.PacFeature;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.algorithms.pac.AnytimePTSForMLPac;
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

	@SuppressWarnings("rawtypes")
	public static void ExtractFeaturesToFile(SearchResultImpl searchResult, SearchDomain domain, Class domainClass,
			int attemptCounter, int instance, double optimalCost, double inputEpsilon) {

		String domainName = domainClass.getSimpleName();
		int problemInstance = instance;
		int attempt = attemptCounter++;
		
		Map<PacFeature,Double> features = MLPacFeatureExtractor.extractFeaturesFromSearchResult(searchResult,optimalCost,inputEpsilon);

		double generated = features.get(PacFeature.GENERATED);
		double expanded = features.get(PacFeature.EXPANDED);
		double reopened = features.get(PacFeature.ROPENED);
		double U = features.get(PacFeature.COST);
		double g = features.get(PacFeature.LENGTH);
		double initialH = features.get(PacFeature.H_0);
		
		double g1 = features.get(PacFeature.G_1);
		double h1 = features.get(PacFeature.H_1);
		
		double g2 = features.get(PacFeature.G_2);
		double h2 = features.get(PacFeature.H_2);
		

		boolean isWOptimal = features.get(PacFeature.IS_W_OPT) == 1? true : false;

		String[] lineParts = { domainName, problemInstance + "", attempt + "", generated + "", expanded + "",
				reopened + "", U + "", g + "", initialH + "", g1+ "",h1+ "",g2+ "",h2+ "",optimalCost+"",inputEpsilon +"", isWOptimal + "" };
		String line = String.join(",", lineParts);
		logger.debug("adding new features to table: " + line);
		try {
			output.writeln(line);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static AnytimeSearchAlgorithm getAnytimeAlg() {
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
