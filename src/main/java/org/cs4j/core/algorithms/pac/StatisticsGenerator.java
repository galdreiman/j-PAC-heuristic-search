package org.cs4j.core.algorithms.pac;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.cs4j.core.OutputResult;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.AnytimeSearchNode;
import org.cs4j.core.algorithms.WAStar;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.domains.GridPathFinding;
import org.cs4j.core.experiments.ExperimentUtils;
import org.cs4j.core.experiments.PacPreprocessRunner;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Roni Stern on 28/02/2017.
 */
public class StatisticsGenerator {

    final static Logger logger = Logger.getLogger(StatisticsGenerator.class);



    /**
     * Generate the statistics for this instance
     * @param domain
     * @param anytimeSearcher
     * @return
     * @throws IOException
     */
    public List<Double> runOnInstance(SearchDomain domain,
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
        for(Double h : collector.hToRepresentativeState.keySet()){
            state = domain.unpack(collector.hToRepresentativeState.get(h));
            domain.setInitialState(state);
            result = optimalSolver.search(domain);
            if(result.hasSolution())
                logger.info(h+","+ result.getBestSolution().getCost());
        }
        return null;
    }


    public void generateOpenBasedStatistics(Class domainClass){
        List<Double> resultsData;
        SearchDomain domain;
        SearchResult result;
        OutputResult output = null;

        // Extract domain data
        int fromInstance = DomainExperimentData.get(domainClass,RunType.TRAIN).fromInstance;
        int toInstance = DomainExperimentData.get(domainClass,RunType.TRAIN).toInstance;
        String inputPath = DomainExperimentData.get(domainClass,RunType.TRAIN).inputPath;
        Map<String, String> domainParams = new HashMap<>();

        // Construct a variant of A* that records also the h value of the start state
        SearchAwarePACSearch astar = new SearchAwarePACSearchImpl();
        logger.info("Start collecting statistics");
        try {
            // Print the output headers
            output = new OutputResult(DomainExperimentData.get(domainClass,RunType.TRAIN).outputPath,
                    "Preprocess_", -1, -1, null, false,true);
            String[] resultColumnNames = { "InstanceID", "h*(s)", "h(s)", "h*/h" };
            String toPrint = String.join(",", resultColumnNames);
            output.writeln(toPrint);

            // Get the domains constructor
            Constructor<?> cons = ExperimentUtils.getSearchDomainConstructor(domainClass);
            logger.info("Start running search for " + (fromInstance - toInstance + 1) +" instances:");
            for (int i = fromInstance; i <= toInstance; ++i) {
                try {
                    logger.info("Running the " + i +"'th instance");
                    // Read domain from file
                    domain = ExperimentUtils.getSearchDomain(inputPath, domainParams, cons, i);
                    resultsData= this.runOnInstance(domain, astar);
                    resultsData.add(0, (double) i);
                    output.appendNewResult(resultsData.toArray());
                    output.newline();

                } catch (OutOfMemoryError e) {
                    logger.error("PacPreprocessRunner OutOfMemory :-( ", e);
                    logger.error("OutOfMemory in:" + astar.getName() + " on:" + domainClass.getName());
                }
            }
        } catch (IOException e1) {
        } finally {
            output.close();
        }
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
                if(this.incumbent/this.fmin==1); // Only halt if optimal
            throw new PACConditionSatisfied(this);
        }

        @Override
        public void setIncumbent(double incumbent, List<AnytimeSearchNode> openNodes) {
            this.incumbent=incumbent;
            if(this.fmin!=-1)
                if(this.incumbent/this.fmin==1); // Only halt if optimal
            throw new PACConditionSatisfied(this);
        }

        @Override
        public boolean shouldStop(SearchResult incumbentSolution) {
            return false;
        }

        @Override
        public void setup(SearchDomain domain, double epsilon, double delta) {
            this.hToCount = new HashedMap();
            this.hToRepresentativeState = new HashedMap();
            this.randomGenerator = new Random();
        }
    }


    public static void main(String[] args) throws IOException {
        StatisticsGenerator g = new StatisticsGenerator();


        SearchDomain domain = ExperimentUtils.getSearchDomain(GridPathFinding.class,1);
        SearchAwarePACSearch anytimeSearcher = new SearchAwarePACSearchImpl();
        g.runOnInstance(domain, anytimeSearcher);
    }
}
