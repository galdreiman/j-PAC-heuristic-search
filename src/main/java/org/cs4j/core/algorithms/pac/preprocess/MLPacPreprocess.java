package org.cs4j.core.algorithms.pac.preprocess;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.cs4j.core.AnytimeSearchAlgorithm;
import org.cs4j.core.MLPacFeatureExtractor;
import org.cs4j.core.MLPacFeatureExtractor.PacFeature;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.domains.DockyardRobot;
import org.cs4j.core.domains.GridPathFinding;
import org.cs4j.core.domains.Pancakes;
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

		Class[] domains = { VacuumRobot.class, GridPathFinding.class,   Pancakes.class};

		double[] inputEpsilon = {0.1, 0.2, 0.3, }; // TODO: get input from user
													// (from console,
		// e.g. args[] or whatever)

		for (Class domainClass : domains) {
			for (double epsilon : inputEpsilon) {

				String outFile = DomainExperimentData.get(domainClass, RunType.TRAIN).outputPreprocessPath;
				try {
					output = new OutputResult(outFile, "MLPacPreprocess_e"+epsilon, true);
				} catch (IOException e1) {
					logger.error("Failed to create output ML PAC preprocess csv file at: " + outFile, e1);
				}

				String tableHeader = MLPacFeatureExtractor.getFeaturesHeader();
				try {
					output.writeln(tableHeader);
				} catch (IOException e1) {
					logger.error("Failed to write header to output ML preprocess table: " + tableHeader, e1);
				}

				logger.info("Running anytime for domain " + domainClass.getName());
				try {
					logger.info("Solving " + domainClass.getName());

					// -------------------------------------------------
					// 1. load PAC statistics (to get optimal solutions
					PACUtils.getPACStatistics(domainClass);

					// --------------------------------------------------------------
					// 2. run anytime search and for every solution collect
					// features
					SearchDomain domain;
					Map<String, String> domainParams = new TreeMap<>();
					Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
					int fromInstance = DomainExperimentData.get(domainClass, RunType.TRAIN).fromInstance;
					int toInstance = DomainExperimentData.get(domainClass, RunType.TRAIN).toInstance;
					String inputPath = DomainExperimentData.get(domainClass, RunType.TRAIN).inputPath;

					AnytimeSearchAlgorithm algorithm = getAnytimeAlg(epsilon);

					for (int i = fromInstance; i <= toInstance; ++i) {
						logger.info("\rSolving " + domainClass.getName() + "\t instance " + i);
						domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
						double optimalCost = PACUtils.getOptimalSolution(domainClass, i);

						int problemAttemptIndx = 1;
						SearchResultImpl result = (SearchResultImpl) algorithm.search(domain);

						// if another solution is possible - continue searing
						while (result.hasSolution()) {
							// extract feature out of every solution:
							ExtractFeaturesToFile(result, domain, domainClass, problemAttemptIndx++, i, optimalCost,
									epsilon);

							// continue to search another solution:
							result = (SearchResultImpl) algorithm.continueSearch();
						}

						System.out.println("------------------");
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
	}

	@SuppressWarnings("rawtypes")
	public static void ExtractFeaturesToFile(SearchResultImpl searchResult, SearchDomain domain, Class domainClass,
			int attemptCounter, int instance, double optimalCost, double inputEpsilon) {

//		String domainName = domainClass.getSimpleName();
//		int problemInstance = instance;
//		int attempt = attemptCounter++;

		// "generated,expanded,reopened,cost,g1,h1,g2,h2,g3,h3,w,is-W-opt"

		Map<PacFeature, Double> features = MLPacFeatureExtractor
				.extractFeaturesFromSearchResultIncludeTarget(searchResult, optimalCost, inputEpsilon);

		double generated = features.get(PacFeature.GENERATED);
		double expanded = features.get(PacFeature.EXPANDED);
		double reopened = features.get(PacFeature.ROPENED);
		double U = features.get(PacFeature.COST);

		double g1 = features.get(PacFeature.G_0);
		double h1 = features.get(PacFeature.H_0);

		double g2 = features.get(PacFeature.G_2);
		double h2 = features.get(PacFeature.H_2);

		double g3 = features.get(PacFeature.G_2);
		double h3 = features.get(PacFeature.H_2);

		double w = 1.0 + (Double) searchResult.getExtras().get("epsilon");

		boolean isWOptimal =  features.get(PacFeature.IS_W_OPT).intValue() == 1? true : false;

		String[] lineParts = {generated+"",expanded+"",reopened+"",U+"",g1+"",h1+"",g2+"",h2+"",g3+"",h3+"",w+"",isWOptimal+""};
		String line = String.join(",", lineParts);
		logger.debug("adding new features to table: " + line);
		try {
			output.writeln(line);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static AnytimeSearchAlgorithm getAnytimeAlg(double epsilon) {
		AnytimeSearchAlgorithm algorithm = new AnytimePTS() {
			@Override
			public SearchResult search(SearchDomain domain) {
				double initialH = domain.initialState().getH();
				SearchResult results = super.search(domain);
				results.getExtras().put("epsilon", epsilon);
				results.getExtras().put("initial-h", initialH);
				return results;
			}

			@Override
			protected SearchResultImpl _search() {
				// The result will be stored here
				AnytimeSearchNode goal = null;
				this.result = new SearchResultImpl();
				if(this.totalSearchResults==null)
					this.totalSearchResults=this.result;

				result.getExtras().put("epsilon", epsilon);

				result.startTimer();

				int hAndGCounter = 0;

				// Loop while there is no solution and there are states in the OPEN list
				AnytimeSearchNode currentNode;
				while ((goal == null) && !this.open.isEmpty()) {
					// Take a node from the OPEN list (nodes are sorted according to the 'u' function)
					currentNode = this.open.poll();
					this.removeFromfCounter(currentNode.getF());

					// expand the node (since, if its g satisfies the goal test - it would be already returned)
					goal = expand(currentNode);
					++result.expanded;
					if (result.expanded % 1000000 == 0)
						logger.info("[INFO] Expanded so far " + result.expanded + ", incumbent ="+this.incumbentSolution+", fmin="+this.fmin+",opensize="+this.open.size());

					if(currentNode.getF()==this.fmin)
						this.updateFmin();

					if(hAndGCounter < 3){
						this.result.setExtras("g_" + hAndGCounter, new Double(currentNode.g));
						this.result.setExtras("h_"+hAndGCounter, new Double(currentNode.h));
						++hAndGCounter;
					}
				}
				// Stop the timer and check that a goal was found
				result.stopTimer();

				// If a goal was found: update the solution
				if (goal != null) {
					result.addSolution(constructSolution(goal, this.domain));
				}

				result.setExtras("fmin",this.maxFmin); // Record the lower bound for future analysis @TODO: Not super elegant
				return result;
			}

		};
		return algorithm;
	}
}
