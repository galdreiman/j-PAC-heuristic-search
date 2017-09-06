package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.DP;
import org.cs4j.core.algorithms.pac.AnytimePTS4PAC;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.PACUtils;
import org.cs4j.core.algorithms.pac.conditions.OraclePACCondition;
import org.cs4j.core.pac.conf.PacConfig;

import java.util.SortedMap;

/**
 * Created by Roni Stern on 04/03/2017.
 *
 * Simply run DPS
 */
public class DPSExperiment extends StandardExperiment {
    final static Logger logger = Logger.getLogger(DPSExperiment.class);

    public DPSExperiment() {
        super(new DP("DPS", false, false, false));
    }


    public static void main(String args[]) {

        Class[] domains = PacConfig.instance.onlineDomains();

        double[] epsilons = PacConfig.instance.inputOnlineEpsilons();


        Experiment experiment = new DPSExperiment();
        DPSExperimentRunner runner = new DPSExperimentRunner();
        runner.runExperimentBatch(domains, epsilons);

    }
}