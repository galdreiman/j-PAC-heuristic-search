package org.cs4j.core.algorithms.pac.preprocess;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.domains.Pancakes;

import java.io.IOException;
import java.util.TreeMap;

/**
 * Created by Gal on 17/05/2017.
 */
public class MLPacStatisticsGenerator {
    final static Logger logger = Logger.getLogger(MLPacStatisticsGenerator.class);

    public static void main(String[] args) {
        Class[] domains = {Pancakes.class};//{VacuumRobot.class, DockyardRobot.class, FifteenPuzzle.class};
        OutputResult output=null;
        StatisticsGenerator generator = new StatisticsGenerator();

        for(int i = 10; i <= 45; i += 5) {
            for (Class domainClass : domains) {
                logger.info("Running anytime for domain " + domainClass.getName());
                try {
                    // Prepare experiment for a new domain
                    String format = "./input/pancakes/generated-for-pac-stats-%d";
                    String outputFileDir = String.format(format, i);
                    output = new OutputResult(outputFileDir,
                            "StatisticsGenerator", -1, -1, null, false, true);
                    generator.printResultsHeaders(output,
                            new String[]{"InstanceID", "h", "opt"},
                            new TreeMap<>());
                    generator.run(domainClass, output, new TreeMap<>(), new TreeMap<>());
                    output.close();
                } catch (IOException e) {
                    logger.error(e);
                } finally {
                    if (output != null)
                        output.close();
                }
            }
        }
    }
}
