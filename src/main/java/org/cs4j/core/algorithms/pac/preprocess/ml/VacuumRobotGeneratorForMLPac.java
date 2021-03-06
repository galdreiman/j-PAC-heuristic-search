package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.apache.log4j.Logger;
import org.cs4j.core.generators.VacuumRobotGenerator;
import org.cs4j.core.pac.conf.PacConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Gal Dreiman on 01/08/2017.
 */
public class VacuumRobotGeneratorForMLPac extends VacuumRobotGenerator {
    private final static Logger logger = Logger.getLogger(VacuumRobotGeneratorForMLPac.class);

    public static void main(String[] args)throws IOException {
        int dirtyCountLow = 4, dirtyCountyHigh = 16, dirtyCountDelta = 4, dirtyCountTest = 20;
        generateVacuum(dirtyCountLow, dirtyCountyHigh, dirtyCountDelta,dirtyCountTest);

    }

    public static void generateVacuum(int dirtyCountLow,int dirtyCountyHigh,int dirtyCountDelta, int dirtyCountTest) throws IOException {
        int instancesCount = PacConfig.instance.PredictionNumInstances();
        int mapWidth = 200;
        int mapHeight = 200;
        double obstaclesPercentage = 30;
        int dirtyCount;

//        int dirtyCountLow = 4, dirtyCountyHigh = 16, dirtyCountDelta = 4;

        for(dirtyCount = dirtyCountLow; dirtyCount <= dirtyCountyHigh; dirtyCount += dirtyCountDelta) {
            _generate(instancesCount, mapWidth, mapHeight, obstaclesPercentage, dirtyCount);
        }
        _generate(instancesCount, mapWidth, mapHeight, obstaclesPercentage, dirtyCountTest);
    }

    protected static boolean _generate(int instancesCount, int mapWidth, int mapHeight, double obstaclesPercentage, int dirtyCount) throws IOException {
        String outFileDirStr = /*args[0] != null ? args[0] :*/ "input" + File.separator + "vacuumrobot" + File.separator + "generated-for-pac-stats-" + dirtyCount + "-dirt";
        File outputDirectory = new File(outFileDirStr);
        if (!outputDirectory.isDirectory()) {
            outputDirectory.mkdir();

        }

        //verify if actually needs to generate:
        if(needToGenerateInstances(outFileDirStr,outputDirectory, instancesCount )){
            logger.info("No need to generate new instances. There is enough for the experiment.");
            return true;
        }

        VacuumRobotGenerator generator = new VacuumRobotGenerator();

        for (int i = 0; i < instancesCount; ++i) {
            System.out.println("[INFO] Generating instance # " + (i + 1) + " ...");
            FileWriter fw = new FileWriter(new File(outputDirectory, (i + 1) + ".in"));
            fw.write(generator.generateInstance(mapWidth, mapHeight, obstaclesPercentage, dirtyCount));
            fw.close();
            System.out.println(" Done.");
        }
        return false;
    }
}
