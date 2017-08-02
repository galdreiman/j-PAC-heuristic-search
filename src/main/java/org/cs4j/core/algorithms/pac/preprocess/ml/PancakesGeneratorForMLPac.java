package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.cs4j.core.domains.Utils;
import org.cs4j.core.generators.PancakesGenerator;
import org.cs4j.core.pac.conf.PacConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Gal Dreiman on 01/08/2017.
 */
public class PancakesGeneratorForMLPac extends PancakesGenerator{

    public static void main(String[] args) throws IOException{
        int pancakesNumLow = 20;
        int pancakesNumHigh = 45;
        int pancakesDelta = 5;
        generatePancakes(pancakesNumLow, pancakesNumHigh,pancakesDelta );
    }

    public static void generatePancakes(int pancakesNumLow, int pancakesNumHigh, int pancakesDelta) throws IOException {
        int size;
        String previousInstancesDir = null;
        int previousInstancesCount = 0;



        for(int pancakesNum = pancakesNumLow; pancakesNum <= pancakesNumHigh; pancakesNum += pancakesDelta) {
            // In case no arguments were given - let's specify them here

            System.out.println("[WARNING] Using local arguments");
            // Output directory
            String outDir = "input"+ File.separator+"pancakes"+File.separator+"generated-" + pancakesNum;
            // Count of pancakes (number of instances)
            int instancesCount = PacConfig.instance.numInstances();
            // Size of problem
            size = pancakesNum;


            File outputDirectory = new File(outDir);
            if (!outputDirectory.isDirectory()) {
                outputDirectory.mkdir();
            }

            // The size must be at least 2
            if (size < 2) {
                throw new NumberFormatException();
            }


            // This set is used in order to avoid duplicates
            Set<String> instances = new HashSet<>();

            // Fill in previous instances (in order to avoid duplicates)
            if (previousInstancesDir != null) {
                File prev = new File(previousInstancesDir);
                if (prev.exists()) {
                    for (File current : prev.listFiles(pathname -> pathname.getName().endsWith("in"))) {
                        instances.add(Utils.fileToString(current));
                        ++previousInstancesCount;
                    }
                }
            }

            // Now, create the problems
            PancakesGenerator generator = new PancakesGenerator();
            // Loop over the required number of instances
            for (int i = 0; i < instancesCount; ++i) {
                int pancakeNumber = i + 1;
                System.out.println("[INFO] Generating instance # " + pancakeNumber + " ...");
                FileWriter fw = new FileWriter(new File(outputDirectory, pancakeNumber + ".in"));
                String instance = null;
                while (instance == null || instances.contains(instance)) {
                    instance = generator.generateInstance(size);
                }
                instances.add(instance);
                fw.write(instance);
                fw.close();
                System.out.println(" Done.");
            }
            assert instances.size() == instancesCount + previousInstancesCount;
        }
    }
}
