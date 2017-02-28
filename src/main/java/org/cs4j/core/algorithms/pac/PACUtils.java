package org.cs4j.core.algorithms.pac;

import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

import java.io.BufferedReader;
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

    public static Map<Integer,Double> getOptimalSolutions(Class domainClass)
    {
        String inputFile = DomainExperimentData.get(domainClass,RunType.TRAIN).inputPath+"/optimalSolutions.in";
        return parseFileWithPairs(inputFile);
    }
    public static Map<Integer,Double> getInitialHValues(Class domainClass)
    {
        String inputFile = DomainExperimentData.get(domainClass,RunType.TRAIN).inputPath+"/initialHValues.csv";
        return parseFileWithPairs(inputFile);
    }

    /**
     * Internal helper function that parses a file with 2 columsn to a map
     * @param inputFile input file
     */
    private static Map<Integer,Double> parseFileWithPairs(String inputFile){
        Map<Integer,Double> keyToValue= new TreeMap<>();
        try{
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line = reader.readLine();
            String[] parts;

            // Handle first line if headers, if such exists
            parts = line.split(",");
            if(isDouble(parts[1])==false)// Then this is a header row and should ignore it
                line = reader.readLine();

            while(line!=null){
                parts = line.split(",");
                keyToValue.put(Integer.parseInt(parts[0]), Double.parseDouble(parts[1]));
                line = reader.readLine();
            }
            reader.close();
        }
        catch(IOException e){
            logger.error("Cannot load statistics",e);
            throw new RuntimeException(e);
        }
        return keyToValue;
    }

    public static PACStatistics getStatisticsFile(PACCondition condition, Class domainClass){
        return domainToPACStatistics.get(domainClass);
    }
    public static void setStatisticFile(PACCondition condition, Class domainClass,PACStatistics statistics){
        domainToPACStatistics.put(domainClass,statistics);
    }


    /**
     * Check if a string is convertable to double
     */
    private static boolean isDouble(String text){
        try
        {
            Double.parseDouble(text);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }

    /**
     * Load PAC statistics for the given domain
     * @param domainClass the class of domains for which to load statistics
     */
    public static void loadPACStatistics(Class domainClass)
    {
        PACStatistics pacStatistics = new PACStatistics();
        pacStatistics.instanceToInitialH = getInitialHValues(domainClass);
        pacStatistics.instanceToOptimal= getOptimalSolutions(domainClass);
        setStatisticFile(null,domainClass,pacStatistics);
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
}
