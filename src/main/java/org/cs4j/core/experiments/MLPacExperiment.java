package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.algorithms.pac.AnytimePTS4PAC;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.conditions.MLPacCondition;
import org.cs4j.core.algorithms.pac.conditions.OpenBasedPACCondition;
import org.cs4j.core.algorithms.pac.conditions.OraclePACCondition;
import org.cs4j.core.domains.*;

public class MLPacExperiment extends StandardExperiment {

	private final static Logger logger = Logger.getLogger(MLPacExperiment.class);

	public MLPacExperiment() {
		super(new PACSearchFramework());
		logger.info("Init ML-PAC Experiment");
		PACSearchFramework psf = (PACSearchFramework) this.searchAlgorithm;
		psf.setAnytimeSearchClass(AnytimePTS4PAC.class);
		psf.setPACConditionClass(MLPacCondition.class);
		psf.setAdditionalParameter("anytimeSearch", AnytimePTS4PAC.class.getName());
	}

	@SuppressWarnings("rawtypes")
	public static void main(String args[]) {

		Class[] domains = { Pancakes.class /*VacuumRobot.class, GridPathFinding.class,   Pancakes.class */};
		Class[] pacConditions = { MLPacCondition.class,  /*OpenBasedPACCondition.class*/};

		double[] epsilons = { 0.3 };
		double[] deltas = { 0.9 /*0.0, 0.2, 0.5, 0.9, 0.99 */};

		Experiment experiment = new MLPacExperiment();
		PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
		runner.runExperimentBatch(domains, pacConditions, epsilons, deltas, experiment);

	}

}
