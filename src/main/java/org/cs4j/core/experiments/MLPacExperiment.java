package org.cs4j.core.experiments;

import org.cs4j.core.algorithms.pac.AnytimePTSForMLPac;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.conditions.MLPacCondition;
import org.cs4j.core.domains.VacuumRobot;

public class MLPacExperiment extends StandardExperiment {

	public MLPacExperiment() {
		super(new PACSearchFramework());

		PACSearchFramework psf = (PACSearchFramework) this.searchAlgorithm;
		psf.setAnytimeSearchClass(AnytimePTSForMLPac.class);
		psf.setPACConditionClass(MLPacCondition.class);
	}

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {

		Class[] domains = { VacuumRobot.class };
		Class[] pacConditions = { MLPacCondition.class};
		double[] epsilons = {0.2};
		double[] deltas = {0.1};

		PACSearchFramework psf = new PACSearchFramework();
		psf.setAdditionalParameter("anytimeSearch", AnytimePTSForMLPac.class.getName());
		Experiment experiment = new StandardExperiment(psf);

		PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
		runner.runExperimentBatch(domains, pacConditions, epsilons, deltas, experiment);
	}

}
