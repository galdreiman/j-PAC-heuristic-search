package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.pac.AnytimePTS4PAC;
import org.cs4j.core.algorithms.pac.OraclePACCondition;
import org.cs4j.core.algorithms.pac.PACSearchFramework;
import org.cs4j.core.algorithms.pac.PACUtils;

import java.util.SortedMap;

/**
 * Created by Roni Stern on 01/03/2017.
 *
 * Run a PAC experiment with the Oracle PAC condition
 */
public class OracleExperiment extends StandardExperiment {

    final static Logger logger = Logger.getLogger(OracleExperiment.class);

    public OracleExperiment() {
        super(new PACSearchFramework());

        PACSearchFramework psf = (PACSearchFramework)this.searchAlgorithm;
        psf.setAnytimeSearchAlgorithm(new AnytimePTS4PAC());
        psf.setPACCondition(new OraclePACCondition());
    }

    @Override
    public void run(SearchDomain instance,  OutputResult output, int instanceId, SortedMap<String, Object> runParams) {
        // @TODO: Very ineffecicent: parse input file for every instance
        Double optimalSolution = PACUtils.getOptimalSolutions(instance.getClass()).get(instanceId);

        if(optimalSolution==null){
            logger.info("No known optimal solution for instance "+instance + " in domain "+instance.getClass().getSimpleName());
            return;
        }
        PACSearchFramework psf = (PACSearchFramework) searchAlgorithm;
        OraclePACCondition pacCondition = (OraclePACCondition)psf.getPACCondition();
        pacCondition.setOptimalSolution(optimalSolution);

        super.run(instance,output,instanceId,runParams);
    }
}
