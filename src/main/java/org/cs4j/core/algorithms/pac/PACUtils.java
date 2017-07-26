package org.cs4j.core.algorithms.pac;

import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.pac.conditions.TrivialPACCondition;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Roni Stern on 26/02/2017.
 *
 * A place for utility functions and constants relevant to PAC Search
 */
public class PACUtils {

    final static Logger logger = Logger.getLogger(TrivialPACCondition.class);

    // Maps a domain to a PAC statistics object, used later by the PAC conditions
    private static Map<Class, PACStatistics> domainToPACStatistics
            = new HashMap<>();


    public static PACStatistics getPACStatistics(Class domainClass)
    {
        // If already cached, no need to parse from disk
        if(domainToPACStatistics.containsKey(domainClass))
            return domainToPACStatistics.get(domainClass);

        String statisticsFile = DomainExperimentData.get(domainClass,RunType.TRAIN).inputPath
                +File.separator+PACStatistics.STATISTICS_FILE_NAME;
        PACStatistics statistics =  parsePACStatisticsFile(statisticsFile);
        domainToPACStatistics.put(domainClass,statistics);
        return statistics;
    }

    public static PACStatistics getPACStatistics(Class domainClass, RunType rt, int domainParam)
    {
        // If already cached, no need to parse from disk
        if(domainToPACStatistics.containsKey(domainClass))
            return domainToPACStatistics.get(domainClass);

        String statisticsFile = String.format(DomainExperimentData.get(domainClass,rt).inputPathFormat
                +File.separator+PACStatistics.STATISTICS_FILE_NAME, domainParam);
        PACStatistics statistics =  parsePACStatisticsFile(statisticsFile);
        domainToPACStatistics.put(domainClass,statistics);
        return statistics;

    }

    public static Double getOptimalSolution(Class domainClass, int instance){
        return getPACStatistics(domainClass).instanceToOptimal.get(instance);
    }

    /**
     * Internal helper function that parses the file with the basic PAC statistics (h(s), h*(s))
     * @param inputFile input file for this domain
     */
    private static PACStatistics parsePACStatisticsFile(String inputFile){
        Map<Integer,Double> instanceToInitialH= new TreeMap<>();
        Map<Integer,Double> instanceToOptimal= new TreeMap<>();
        try{
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line;
            String[] parts;

            // Handle first line if headers, if such exists
            reader.readLine(); // Ignore the first row - it is for the column headers
            line = reader.readLine();
            int instanceId;
            Double optimal;
            Double initialH;
            while(line!=null){
                parts = line.split(",");
                instanceId = (int)(Double.parseDouble(parts[0]));
                initialH = Double.parseDouble(parts[1]);
                optimal = Double.parseDouble(parts[2]);

                instanceToOptimal.put(instanceId,optimal);
                instanceToInitialH.put(instanceId,initialH);
                line = reader.readLine();
            }
            reader.close();
        }
        catch(IOException e){
            logger.error("Cannot load statistics",e);
            throw new RuntimeException(e);
        }

        PACStatistics statistics = new PACStatistics();
        statistics.instanceToOptimal=instanceToOptimal;
        statistics.instanceToInitialH=instanceToInitialH;

        return statistics;
    }

    /**
     * Read from disk the h to optimal statistics for this domain
     */
    public static List<Tuple<Double,Double>> getHtoOptimalTuples(Class domainClass)
    {
        DomainExperimentData domainDetails = DomainExperimentData.get(domainClass,RunType.TRAIN);
        String inputFile = domainDetails.outputPreprocessPath+File.separator+"openBasedStatistics.csv";
        List<Tuple<Double,Double>> tuples = new ArrayList<>();
        String[] parts;
        double h,opt;
        // Read the tuples from the statistics file
        try{
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            reader.readLine();// Headers row
            String line = reader.readLine();
            int instance;
            while(line!=null) {
                parts = line.split(",");
                instance = Integer.parseInt(parts[0]);
                if((instance>= domainDetails.fromInstance)&&(instance<=domainDetails.toInstance)) {
                    h = Double.parseDouble(parts[1]);
                    opt = Double.parseDouble(parts[2]);
                    if(opt>=0) // In some cases we didn't find the optimal solution, so skip thoseS
                        tuples.add(new Tuple<>(h,opt));
                }

                line = reader.readLine();
            }
        }
        catch(IOException e){
            logger.error(e);
            throw new RuntimeException(e);
        }

        // Sort tuples according to the h values
        tuples.sort(new Comparator<Tuple<Double, Double>>() {
            @Override
            public int compare(Tuple<Double, Double> o1, Tuple<Double, Double> o2) {
                if (o1._1 < o2._1)
                    return -1;
                else if (o1._1 > o2._1)
                    return 1;
                else
                    return 0;
            }
        });

        return  tuples;
    }

    public static PACStatistics getStatisticsFile(PACCondition condition, Class domainClass){
        return domainToPACStatistics.get(domainClass);
    }
    public static void setStatisticFile(PACCondition condition, Class domainClass,PACStatistics statistics){
        domainToPACStatistics.put(domainClass,statistics);
    }


    /**
     * Computes a cumulative distribution function (CDF) for a list of values
     *
     * @return a map of solution cost value to the corresponding CDF.
     */
    public static SortedMap<Double, Double> computeCDF(List<Double> values){
        // Building the PDF (  cost -> prob. that optimal is less than or equal to cost)

        // First, count how many instances were for every h value
        SortedMap<Double, Double> pdf = new TreeMap<Double, Double>();
        for(Double value : values){
            if(pdf.containsKey(value)==false)
                pdf.put(value,1.0);
            else
                pdf.put(value,(pdf.get(value)+1));
        }

        // Now, divide by the total number of instances, to get the "probability"
        for(Double value : pdf.keySet())
            pdf.put(value, pdf.get(value)/values.size());
        return pdfToCdf(pdf);


    }

    /**
     * Crearing a CDF for a given PDF
     * @param pdf the given PDF
     * @return the resulting CDF
     */
    public static SortedMap<Double, Double> pdfToCdf(SortedMap<Double, Double> pdf) {
        // Building the CDF (cumulative)
        SortedMap<Double, Double>ratioToCDF = new TreeMap<>();
        Double oldCDFValue=0.0;
        for(Double cost : pdf.keySet()) {
            ratioToCDF.put(cost, pdf.get(cost) + oldCDFValue);
            oldCDFValue = ratioToCDF.get(cost);
        }

        // Accuracy issues
        if(oldCDFValue!=1.0){
            assert Math.abs(oldCDFValue-1.0)<0.0001; // Verifying this is just an accuracy issue
            ratioToCDF.put(ratioToCDF.lastKey(),1.0);
        }

        return ratioToCDF;
    }

    /**
     * Loads the statistics from disk to memory
     */
    public static void loadPACStatistics(Class domainClass) {
        getPACStatistics(domainClass);
    }
}
