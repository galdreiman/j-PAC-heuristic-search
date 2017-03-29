package org.cs4j.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import weka.core.Attribute;

public class MLPacFeatureExtractor {

	private final static Logger logger = Logger.getLogger(MLPacFeatureExtractor.class);
	
	public enum PacFeature{
		GENERATED, EXPANDED, ROPENED, COST, LENGTH, G_0,H_0,G_1,H_1,G_2,H_2,IS_W_OPT;
		
		
	}
	
	public static String getFeaturesHeader(){
		return "domain,instance,index,generated,expanded,reopened,cost,g1,h1,g2,h2,g3,h3,oprimal_cost,w,is-W-opt";
	}
	
	public static Map<PacFeature,Double> extractFeaturesFromSearchResultIncludeTarget(SearchResult result, double optimalCost, double inputEpsilon){
		logger.debug("MLPacFeatureExtractor:extractFeaturesFromSearchResult");
		Map<PacFeature,Double> features = extractFeaturesFromSearchResult(result);
		
		Double U = result.getBestSolution().getCost();
		int g = result.getBestSolution().getLength();
		
		features.put(PacFeature.IS_W_OPT, new Double(isWOpttimal(U, g, optimalCost, inputEpsilon)==true? 1.0 : 0.0));
		
		return features;
	}
	
	public static Map<PacFeature,Double> extractFeaturesFromSearchResult(SearchResult result){
		Map<PacFeature,Double> features = new HashMap<>();
		
		features.put(PacFeature.GENERATED, new Double(result.getGenerated()));
		features.put(PacFeature.EXPANDED, new Double(result.getExpanded()));
		features.put(PacFeature.ROPENED, new Double(result.getReopened()));
		Double U = result.getBestSolution().getCost();
		features.put(PacFeature.COST, new Double(U));
		int g = result.getBestSolution().getLength();
		features.put(PacFeature.LENGTH, new Double(g));
		features.put(PacFeature.H_0, new Double((double) result.getExtras().get("h_0")));
		features.put(PacFeature.G_1, new Double((double) result.getExtras().get("g_1")));
		features.put(PacFeature.H_1, new Double((double) result.getExtras().get("h_1")));
		features.put(PacFeature.G_2, new Double((double) result.getExtras().get("g_2")));
		features.put(PacFeature.H_2, new Double((double) result.getExtras().get("h_2")));
		
		return features;
	}
	
	
	
	public static String getHeaderLineFeatures() {
		String[] attributes = { "domain", "instance", "index", "generated", "expanded", "reopened", "cost", "g1",
				"h1","g2", "h2", "g3","h3","oprimal_cost", "w", "is-W-opt" };
		return String.join(",", attributes);
	}



	

	public static boolean isWOpttimal(double U, int g, double optimalCost, double inputEpsilon) {
		// a solution consider as W-optimal iff:
		// h*(n)(1+epsilon) < U
		
		logger.debug("h*(n)[" + optimalCost +"](1+epsilon)[" + (1+inputEpsilon) +"] < U[" +U+"]");

		return  (optimalCost * (1+inputEpsilon) ) < U;
	}

	public static ArrayList<Attribute> getAttributes() {
		String[] lables = MLPacFeatureExtractor.getHeaderLineFeatures().split(",");
		ArrayList<Attribute> atts = new ArrayList<>();
		for(int i = 3; i < lables.length -1; ++i){
			atts.add(new Attribute(lables[i]));
		}
		return atts;
	}
}
