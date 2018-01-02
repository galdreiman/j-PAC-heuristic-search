package org.cs4j.core.algorithms.pac;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.SearchResult;
import org.cs4j.core.algorithms.DP;
import org.cs4j.core.algorithms.PHS;

import java.util.Random;

/**
 * Created by Gal Dreiman on 25/12/2017.
 */
public class CommonSearchFramework extends PACSearchFramework {

    protected Random random;
    protected PHS PHSSearchAlgorithm;
    protected DP DPSSearchAlgorithm;

    public CommonSearchFramework(){
        this.random = new Random();
        this.PHSSearchAlgorithm = new PHS();
        this.DPSSearchAlgorithm = new DP("DPS", false, false, false);
    }

    @Override
    public String getName() {
        return "CommonSearchFramework";
    }

    @Override
    public SearchResult search(SearchDomain domain) {

        double prob = this.random.nextDouble();
        SearchResult result = null;

        if(prob < this.delta){
            // run greedy best first search (also known as Pure Heursitic Search)
            result = this.PHSSearchAlgorithm.search(domain);
        } else {
            // run DPS
            result = this.DPSSearchAlgorithm.search(domain);
        }

        return result;
    }
}
