package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.algorithms.pac.AnytimePTS4PAC;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.conditions.*;
import org.cs4j.core.domains.*;
import org.cs4j.core.pac.conf.PacConfig;

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

		Class[] domains = PacConfig.instance.onlineDomains();
		Class[] pacConditions = PacConfig.instance.onlinePacConditions();

		double[] epsilons = PacConfig.instance.inputOnlineEpsilons();
		double[] deltas = PacConfig.instance.inputOnlineDeltas();


		Experiment experiment = new MLPacExperiment();
		PACOnlineExperimentRunner runner = new PACOnlineExperimentRunner();
		runner.runExperimentBatch(domains, pacConditions, epsilons, deltas, experiment);

	}

}
