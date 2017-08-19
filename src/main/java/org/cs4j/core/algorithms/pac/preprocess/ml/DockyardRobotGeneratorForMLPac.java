package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.apache.log4j.Logger;
import org.cs4j.core.algorithms.pac.conditions.MLPacCondition;
import org.cs4j.core.generators.DockyardRobotGenerator;
import org.cs4j.core.pac.conf.PacConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Gal Dreiman on 01/08/2017.
 */
public class DockyardRobotGeneratorForMLPac extends DockyardRobotGenerator {
    private final static Logger logger = Logger.getLogger(DockyardRobotGeneratorForMLPac.class);


    public static void main(String[] args) throws IOException{
        int boxesCountLow = 4, boxesCountHigh = 7, boxesCountDelta =1;
        generate(boxesCountLow,boxesCountHigh,boxesCountDelta);
    }

    public static void generate(int boxesCountLow, int boxesCountHigh, int boxesCountDelta) throws IOException {
        int instancesCount = PacConfig.instance.PredictionNumInstances();
        int locationsCount = 4;
        int cranesCount = 4;
        int pilesCount = 4;
        // A single robot is enforced!
        int robotsCount = 1;

        int boxesCount ;
//        int boxesCountLow = 4, boxesCountHigh = 7, boxesCountDelta =1;

        // Some assertions for current implementation
        assert cranesCount == locationsCount;
        assert pilesCount == locationsCount;
        assert robotsCount == 1;


        for(boxesCount = boxesCountLow; boxesCount <= boxesCountHigh; boxesCount += boxesCountDelta) {
            String outputDirectoryPath = "input" + File.separator + "dockyardrobot" + File.separator + "generated-" + boxesCount + "-boxes";

            File outputDirectory = new File(outputDirectoryPath);
            if (!outputDirectory.isDirectory()) {
                outputDirectory.mkdir();
            }

            //verify if actually needs to generate:
            if(needToGenerateInstances(outputDirectoryPath,outputDirectory, instancesCount )){
                logger.info("No need to generate new instances. There is enough for the experiment.");
                return;
            }

            // This set is used in order to avoid duplicates
            Set<String> instances = new HashSet<>();
            // Now, create the problems
            DockyardRobotGenerator generator = new DockyardRobotGenerator();
            // Loop over the required number of instances
            for (int i = 0; i < instancesCount; ++i) {
                int problemNumber = i + 1;
                System.out.println("[INFO] Generating instance # " + problemNumber + " ...");
                FileWriter fw = new FileWriter(new File(outputDirectory, problemNumber + ".in"));
                String instance = null;
                while (instance == null || instances.contains(instance)) {
                    instance = generator.generateInstance(locationsCount, cranesCount, boxesCount, pilesCount, robotsCount);
                }
                instances.add(instance);
                fw.write(instance);
                fw.close();
                System.out.println(" Done.");
            }
            assert instances.size() == instancesCount;
        }
    }

}
