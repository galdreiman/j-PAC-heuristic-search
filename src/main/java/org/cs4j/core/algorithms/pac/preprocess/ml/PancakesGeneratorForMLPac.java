package org.cs4j.core.algorithms.pac.preprocess.ml;

import org.apache.log4j.Logger;
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

    private final static Logger logger = Logger.getLogger(PancakesGeneratorForMLPac.class);

    public static void main(String[] args) throws IOException{
        int pancakesNumLow = 20;
        int pancakesNumHigh = 40;
        int pancakesDelta = 5;
        int pancakesTest = 45;
        generatePancakes(pancakesNumLow, pancakesNumHigh,pancakesDelta,pancakesTest );
    }

    public static void generatePancakes(int pancakesNumLow, int pancakesNumHigh, int pancakesDelta,int pancakesTest) throws IOException {
        _generate(pancakesTest);
        for(int pancakesNum = pancakesNumLow; pancakesNum <= pancakesNumHigh; pancakesNum += pancakesDelta) {
            _generate(pancakesNum);
        }
    }

    protected static boolean _generate(int pancakesNum) throws IOException {
        // In case no arguments were given - let's specify them here

        System.out.println("[WARNING] Using local arguments");
        // Output directory
        String outDir = "input"+ File.separator+"pancakes"+File.separator+"generated-for-pac-stats-" + pancakesNum + "-dirts";
        // Count of pancakes (number of instances)
        int instancesCount = PacConfig.instance.PredictionNumInstances();

        File outputDirectory = new File(outDir);
        if (!outputDirectory.isDirectory()) {
            outputDirectory.mkdir();
        }

        //verify if actually needs to generate:
        if(needToGenerateInstances(outDir,outputDirectory, instancesCount )){
            logger.info("No need to generate new instances. There is enough for the experiment.");
            return true;
        }

        // The size must be at least 2
        if (pancakesNum < 2) {
            throw new NumberFormatException();
        }


        // This set is used in order to avoid duplicates
        Set<String> instances = new HashSet<>();

        // Now, create the problems
        PancakesGenerator generator = new PancakesGenerator();
        // Loop over the required number of instances
        for (int i = 0; i < instancesCount; ++i) {
            int pancakeNumber = i + 1;
            System.out.println("[INFO] Generating instance # " + pancakeNumber + " ...");
            FileWriter fw = new FileWriter(new File(outputDirectory, pancakeNumber + ".in"));
            String instance = null;
            while (instance == null || instances.contains(instance)) {
                instance = generator.generateInstance(pancakesNum);
            }
            instances.add(instance);
            fw.write(instance);
            fw.close();
            System.out.println(" Done.");
        }
        return false;
    }
}
