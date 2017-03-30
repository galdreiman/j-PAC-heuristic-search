package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.algorithms.pac.AnytimePTS4PAC;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.conditions.MLPacCondition;
import org.cs4j.core.domains.VacuumRobot;

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

		Class[] domains = { VacuumRobot.class, };
		Class[] pacConditions = { MLPacCondition.class};//, DockyardRobot.class, FifteenPuzzle.class };
		double[] epsilons = { 0.2, 0.5 };
		double[] deltas = { 0.2, 0.5, 0.7 };

		Experiment experiment = new MLPacExperiment();
		PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
		runner.runExperimentBatch(domains, pacConditions, epsilons, deltas, experiment);

	}

}
