package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.cs4j.core.generators.GridPathFindingGenerator;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.pac.conf.PacConfig;

import java.io.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by Gal Dreiman on 01/08/2017.
 */
public class GridPathFindingGeneratorForMLPac extends GridPathFindingGenerator {

    public static void main(String[] args) throws IOException{
        double obstaclesPercentageLow = 75, obstaclesPercentageHigh = 85, obstaclesPercentageDelta = 5;
        generateMLPacInstances(obstaclesPercentageLow,obstaclesPercentageHigh,obstaclesPercentageDelta);
    }

    public static void generateMLPacInstances(double obstaclesPercentageLow, double obstaclesPercentageHigh, double obstaclesPercentageDelta) throws IOException {
        Random rand = new Random();
        String localArgs[] = new String[3];


        double obstaclesPercentage;



        String[] outputPaths =
                new String[]{
                        "input/gridpathfinding/brc202d.map",
//                        "input/gridpathfinding/den400d.map",
//                        "input/gridpathfinding/ost003d.map"
                };
        String[] mapFiles =
                new String[]{
                        "input/gridpathfinding/raw/maps/brc202d.map",
//                        "input/gridpathfinding/raw/maps/den400d.map",
//                        "input/gridpathfinding/raw/maps/ost003d.map"
                };


        for (int i = 0; i < outputPaths.length; ++i) {
            localArgs[0] = outputPaths[i];
            localArgs[1] = mapFiles[i];
            localArgs[2] = PacConfig.instance.pacPreprocessNumInstances() +"";


            // Read the map file
            String mapFile = localArgs[1];
            if (!(new File(mapFile).exists())) {
                System.out.println("Map file " + mapFile + " doesn't exist");
            }

            for(obstaclesPercentage = obstaclesPercentageLow; obstaclesPercentage <= obstaclesPercentageHigh; obstaclesPercentage += obstaclesPercentageDelta) {

                // Read the output directory
                String outputDir = "input" + File.separator + "gridpathfinding" + File.separator + "brc202d.map"+ File.separator + "grid-obs-presentage-" + ((int)obstaclesPercentage);
                File outputDirectory = new File(outputDir);
                if (!outputDirectory.isDirectory()) {
                    outputDirectory.mkdir();
                }

                // Read required count of instances
                // Required number of instances
                int instancesCount = GridPathFindingGenerator.readIntNumber(localArgs[2], 1, -1, "# of instances");

                GridPathFindingGenerator generator = new GridPathFindingGenerator();

                // Read the map
                GridMap map = generator.readMap(new BufferedReader(new InputStreamReader(new FileInputStream(mapFile))));

                int obstaclesCount = map.countObstacles();
                int requiredObstaclesCount = (int) Math.ceil((obstaclesCount * obstaclesPercentage) / 100.0d);
                generator.fitObstaclesRandomly(map, obstaclesCount, requiredObstaclesCount, rand);


                // This set is used in order to avoid duplicates
                Set<String> instances = new HashSet<>();

                // Loop over the required number of instances
                for (int j = 0; j < instancesCount; ++j) {
                    int problemNumber = j + 1;
                    System.out.println("[INFO] Generating instance # " + problemNumber + " ...");
                    FileWriter fw = new FileWriter(new File(outputDirectory, problemNumber + ".in"));
                    String instance = null;
                    while (instance == null || instances.contains(instance)) {
                        instance = generator.generateInstance(mapFile, map);
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
}
