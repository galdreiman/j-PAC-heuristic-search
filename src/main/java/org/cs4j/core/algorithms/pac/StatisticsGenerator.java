package org.cs4j.core.algorithms.pac;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.domains.*;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.mains.DomainExperimentData;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Created by Roni Stern on 28/02/2017.
 */
public class StatisticsGenerator {

    final static Logger logger = Logger.getLogger(StatisticsGenerator.class);



    /**
     * Generate the statistics for this instance
     * @param domain
     * @return an h to optimal map generated from random states from the search space
     * of solving the given domain.
     */
    private Map<Double,Double> runOnInstance(SearchDomain domain) throws IOException {

        PACSearchFramework psf = new PACSearchFramework();
        psf.setAdditionalParameter("epsilon","0.0");
        psf.setAdditionalParameter("delta","0.0");
        psf.setAnytimeSearchClass(SearchAwarePACSearchImpl.class);
        psf.setPACConditionClass(StatesCollector.class);

        SearchResult result = psf.search(domain);
        StatesCollector collector = (StatesCollector) psf.getPACCondition();

        WAStar optimalSolver = new WAStar();
        optimalSolver.setAdditionalParameter("weight","1.0");
        SearchDomain.State state;
        Map<Double,Double> hToOptimal = new TreeMap<>();
        for(Double h : collector.hToRepresentativeState.keySet()){
            state = domain.unpack(collector.hToRepresentativeState.get(h));
            domain.setInitialState(state);
            result = optimalSolver.search(domain);
            if(result.hasSolution()) {
                hToOptimal.put(h,result.getBestSolution().getCost());
            }
            else{
                hToOptimal.put(h,-1.0);
            }
        }
        return hToOptimal;
    }




    /**
     * Print the headers for the experimental results into the output file
     * @param output
     * @param runParams
     * @throws IOException
     */
    public void printResultsHeaders(OutputResult output, String[] columnHeaders,
                                    SortedMap<String,Object> runParams) throws IOException {
        List<String> runParamColumns = new ArrayList<>(runParams.keySet());
        List<String> columnNames = new ArrayList();
        for(String columnName: columnHeaders)
            columnNames.add(columnName);
        columnNames.addAll(runParamColumns);
        String toPrint = String.join(",", columnNames);
        output.writeln(toPrint);
    }

    public void run(Class domainClass,
                    OutputResult output,
                    SortedMap<String, String> domainParams,
                    SortedMap<String,Object> runParams) {
        SearchDomain domain;
        int fromInstance= DomainExperimentData.get(domainClass,DomainExperimentData.RunType.TRAIN).fromInstance;
        int toInstance= DomainExperimentData.get(domainClass,DomainExperimentData.RunType.TRAIN).toInstance;
        String inputPath = DomainExperimentData.get(domainClass,DomainExperimentData.RunType.TRAIN).inputPath;
        Map<Double,Double> hToOptimal;
        Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
        try {
            // search on this domain and algo and weight the 100 instances
            for (int i = fromInstance; i <= toInstance; ++i) {
                // Read domain from file
                logger.info("\rGenerating statistics for " + domainClass.getName() + "\t instance " + i);
                domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
                hToOptimal = runOnInstance(domain);
                logger.info("Statistics generated!");

                for(Double h : hToOptimal.keySet()) {
                    output.appendNewResult(new Object[]{i,h,hToOptimal.get(h)});
                    output.newline();
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }



    /**
     * Generate statistics
     * @param args
     */
    public static void main(String[] args) {
        Class[] domains = {VacuumRobot.class, DockyardRobot.class, FifteenPuzzle.class};
        OutputResult output=null;
        StatisticsGenerator generator = new StatisticsGenerator();

        for(Class domainClass : domains) {
            logger.info("Running anytime for domain " + domainClass.getName());
            try {
                // Prepare experiment for a new domain
                output = new OutputResult(DomainExperimentData.get(domainClass, DomainExperimentData.RunType.TRAIN).outputPath,
                        "StatisticsGenerator", -1, -1, null, false, true);
                generator.printResultsHeaders(output,
                        new String[]{"InstanceID", "h", "opt"},
                        new TreeMap<>());
                generator.run(domainClass,output,new TreeMap<>(),new TreeMap<>());
                output.close();
        }catch(IOException e){
                logger.error(e);
            }finally{
                if(output!=null)
                output.close();
            }
        }
    }
}
