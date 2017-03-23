package org.cs4j.core.algorithms.pac.preprocess;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.cs4j.core.AnytimeSearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimePTS;
import org.cs4j.core.algorithms.SearchResultImpl;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.domains.VacuumRobot;
import org.cs4j.core.experiments.AnytimeExperimentRunner;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

public class MLPacPreprocess extends AnytimeExperimentRunner {

	

	private final static Logger logger = Logger.getLogger(MLPacPreprocess.class);

	/**
	 * Generate statistics
	 * 
	 * @param args
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String[] args) {
		Class[] domains = { VacuumRobot.class }; // , DockyardRobot.class,
													// FifteenPuzzle.class };

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

					SearchResultImpl results = (SearchResultImpl)algorithm.search(domain);
					int counter = 0;
					while (!results.hasSolution()) {
						algorithm.continueSearch();
						counter ++;
					}
					results = (SearchResultImpl) algorithm.getTotalSearchResults();
					int solutions = results.getSolutions().size();
					System.out.println("------------------");
					System.out.println(solutions);
					System.out.println(counter);
					System.out.println("------------------");
				}

			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	private static AnytimeSearchAlgorithm getAnytimeAlg() {
		AnytimeSearchAlgorithm algorithm = new AnytimePTS() {
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
