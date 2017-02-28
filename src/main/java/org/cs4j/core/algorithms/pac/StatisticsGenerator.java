package org.cs4j.core.algorithms.pac;

import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.domains.GridPathFinding;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.domains.VacuumRobot;
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
     * @param anytimeSearcher
     * @return an h to optimal map generated from random states from the search space
     * of solving the given domain.
     */
    private Map<Double,Double> runOnInstance(SearchDomain domain,
                                      SearchAwarePACSearch anytimeSearcher) throws IOException {


        StatesCollector collector = new StatesCollector();
        PACSearchFramework psf = new PACSearchFramework();
        psf.setAdditionalParameter("epsilon","0.0");
        psf.setAdditionalParameter("delta","0.0");
        psf.setPACCondition(collector);
        psf.setAnytimeSearchAlgorithm(anytimeSearcher);

        SearchResult result = psf.search(domain);

        logger.info("Solution found? " + result.hasSolution());
        for(Double h : collector.hToCount.keySet())
            logger.info(h+","+collector.hToCount.get(h));

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
     * Here we hook onto the SearchAwarePACCondition
     * class to collect a single state for every observed h-value.
     * The tricky part is to collect this in a uniform way (per h value).
     */
    private class StatesCollector implements SearchAwarePACCondition{
        private Random randomGenerator;
        private double fmin=-1;
        private double incumbent=-1;

        // There are the outputs of the collection process: how many states in every h value
        public Map<Double,Integer> hToCount;
        // A state randomly chosen from all states with the same h value
        public Map<Double,PackedElement> hToRepresentativeState;

        @Override
        public void removedFromOpen(AnytimeSearchNode node) {}

        @Override
        /**
         * Count how many nodes are generated for each h value
         */
        public void addedToOpen(AnytimeSearchNode node) {
            if(this.hToCount.containsKey(node.h)) {
                int hCount = this.hToCount.get(node.h) + 1;
                this.hToCount.put(node.h, hCount);

                // We want to choose uniformly from all states with the same h value
                // so we replace with some probability the representative state.
                // To give a fair chance to the states in the beginning of the search
                // the probability to replace decrease with the h-count. (the math works out nice)
                // To account for having mor
                if(this.randomGenerator.nextDouble()<1/hCount)
                    this.hToRepresentativeState.put(node.h,node.packed);
            }
            else {
                this.hToCount.put(node.h, 1);
                this.hToRepresentativeState.put(node.h, node.packed);
            }
        }

        @Override
        public void setFmin(double fmin) {
            this.fmin=fmin;
            if(this.incumbent!=-1)
                if(this.incumbent/this.fmin==1) // Only halt if optimal
                    throw new PACConditionSatisfied(this);
        }

        @Override
        public void setIncumbent(double incumbent, List<AnytimeSearchNode> openNodes) {
            this.incumbent=incumbent;
            if(this.fmin!=-1)
                if(this.incumbent/this.fmin==1) // Only halt if optimal
                    throw new PACConditionSatisfied(this);
        }

        @Override
        public boolean shouldStop(SearchResult incumbentSolution) {
            return false;
        }

        @Override
        public void setup(SearchDomain domain, double epsilon, double delta) {
            this.hToCount = new HashMap();
            this.hToRepresentativeState = new HashMap();
            this.randomGenerator = new Random();
        }
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
        int fromInstance= DomainExperimentData.get(domainClass).fromInstance;
        int toInstance= DomainExperimentData.get(domainClass).toInstance;
        String inputPath = DomainExperimentData.get(domainClass).inputPath;
        Map<Double,Double> hToOptimal;
        Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
        try {
            // search on this domain and algo and weight the 100 instances
            for (int i = fromInstance; i <= toInstance; ++i) {
                // Read domain from file
                logger.info("\rGenerating statistics for " + domainClass.getName() + "\t instance " + i);
                domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
                hToOptimal = runOnInstance(domain,new SearchAwarePACSearchImpl());
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
     * An example of using ExperimentRunner
     * @param args
     */
    public static void main(String[] args) {
        Class[] domains = {GridPathFinding.class,Pancakes.class, VacuumRobot.class};
        OutputResult output=null;
        StatisticsGenerator generator = new StatisticsGenerator();

        for(Class domainClass : domains) {
            logger.info("Running anytime for domain " + domainClass.getName());
            try {
                // Prepare experiment for a new domain
                output = new OutputResult(DomainExperimentData.get(domainClass).outputPath,
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
