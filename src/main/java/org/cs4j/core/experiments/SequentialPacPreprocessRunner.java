package org.cs4j.core.experiments;

import org.cs4j.core.domains.*;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.File;
import java.util.HashMap;

/**
 * Created by Gal on 20/05/2017.
 */
public class SequentialPacPreprocessRunner extends PacPreprocessRunner {

    public static void main(String[] args) {

        SequentialPacPreprocessRunner runner = new SequentialPacPreprocessRunner();
        HashMap domainParams = new HashMap<>();

        // Default classes
        Class[] domains = new Class[]{
                //GridPathFinding.class,
//                FifteenPuzzle.class,
                Pancakes.class,
//                VacuumRobot.class,
//                DockyardRobot.class
        };


        for (Class domainClass : domains) {
            for(int i = 10; i <= 40; i+= 5) {
                logger.info(i + "   Running SequentialPacPreprocessRunner on domain " + domainClass.getSimpleName());

                String inputPath = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).pacInputPathFormat, i);
                String outputPath = String.format(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.ALL).outputPreprocessPathFormat, i);
                verifyOutputPathExistence(outputPath);


                runner.run(domainClass,
                        inputPath,
                        outputPath,
                        DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TRAIN).fromInstance,
                        DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TRAIN).toInstance,
                        domainParams);
                logger.info("Done SequentialPacPreprocessRunner on domain " + domainClass.getSimpleName());
                System.gc();
            }
        }
    }

    private static void verifyOutputPathExistence(String outputPath) {
        File f = new File(outputPath);
        if(!f.exists()){
            f.mkdir();
        }
    }
}
