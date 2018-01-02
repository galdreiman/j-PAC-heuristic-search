package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.algorithms.pac.CommonSearchFramework;
import org.cs4j.core.pac.conf.PacConfig;

/**
 * Created by Gal Dreiman on 25/12/2017.
 */
public class GreedyAndDPSExperiment extends StandardExperiment {

    private final static Logger logger = Logger.getLogger(GreedyAndDPSExperiment.class);


    public GreedyAndDPSExperiment(){
        super(new CommonSearchFramework());
        logger.info("Init GreedyAndDPSExperiment Experiment");
    }

    @Override
    public String[] getResultsHeaders() {
        return new String[] { "InstanceID", "Found", "Depth", "Cost", "Iterations", "Generated",
                "Expanded", "Cpu Time", "Wall Time" };
    }


    public static void main(String args[]) {

        Class[] domains = PacConfig.instance.onlineDomains();

        double[] epsilons = PacConfig.instance.inputOnlineEpsilons();


        Experiment experiment = new GreedyAndDPSExperiment();
        GreedyAndDPSExperimentRunner runner = new GreedyAndDPSExperimentRunner();
        runner.runExperimentBatch(domains, epsilons,experiment);

    }
}
