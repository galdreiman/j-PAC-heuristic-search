package org.cs4j.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import weka.core.Attribute;

public class MLPacFeatureExtractor {

	private final static Logger logger = Logger.getLogger(MLPacFeatureExtractor.class);
	
	public enum PacFeature{
		GENERATED, EXPANDED, ROPENED, COST, LENGTH, G_0,H_0,G_1,H_1,G_2,H_2,W,IS_W_OPT;
		
		public static PacFeature getPacFeature(String name){
			switch (name){
				case "g_0": return G_0;
				case "g_1": return G_1;
				case "g_2": return G_2;
				case "h_0": return H_0;
				case "h_1": return H_1;
				case "h_2": return H_2;

				default:
					throw new IllegalStateException("Bad feature name "+name);
			}
		}
	}
	
	public static String getFeaturesHeader(){
		return "domain,instance,index,generated,expanded,reopened,cost,g1,h1,g2,h2,g3,h3,oprimal_cost,w,is-W-opt";
	}
	
	public static Map<PacFeature,Double> extractFeaturesFromSearchResultIncludeTarget(SearchResult result, double optimalCost, double inputEpsilon){
		logger.debug("MLPacFeatureExtractor:extractFeaturesFromSearchResult");
		Map<PacFeature,Double> features = extractFeaturesFromSearchResult(result);
		
		Double U = result.getBestSolution().getCost();
		
		features.put(PacFeature.IS_W_OPT, new Double(isWOpttimal(U, optimalCost, inputEpsilon)==true? 1.0 : 0.0));
		
		return features;
	}
	
	public static Map<PacFeature,Double> extractFeaturesFromSearchResult(SearchResult result){
		Map<PacFeature,Double> features = new HashMap<>();
		
		features.put(PacFeature.GENERATED, new Double(result.getGenerated()));
		features.put(PacFeature.EXPANDED, new Double(result.getExpanded()));
		features.put(PacFeature.ROPENED, new Double(result.getReopened()));

		Double U = result.getBestSolution().getCost();
		features.put(PacFeature.COST, new Double(U));

		// Get h and g values of the first nodes on the found path
		SearchResult.Solution solution = result.getBestSolution();
		double g=0.0;
		SearchDomain.State parent = null;
		SearchDomain.State current;
		double h;
		int maxPrefix = 3;
		int i=0;
		for(;i<solution.getLength()&& i<maxPrefix;i++){
			current = solution.getStates().get(0);
			if(parent!=null)
				g+=solution.getOperators().get(i).getCost(current, parent);
			h=current.getH();

			features.put(PacFeature.getPacFeature("h_" + i), h);
			features.put(PacFeature.getPacFeature("g_" + i), g);

			parent = current;
		}

		features.put(PacFeature.W, 1.0 + (Double) result.getExtras().get("epsilon"));

		while(i<maxPrefix){
			features.put(PacFeature.getPacFeature("h_" + i), -1.0);
			features.put(PacFeature.getPacFeature("g_" + i), -1.0);
			i++;
		}


		return features;
	}
	
	
	
	public static String getHeaderLineFeatures() {
		String[] attributes = { "domain", "instance", "index", "generated", "expanded", "reopened", "cost", "g1",
				"h1","g2", "h2", "g3","h3","oprimal_cost", "w", "is-W-opt" };
		return String.join(",", attributes);
	}



	

	public static boolean isWOpttimal(double U, double optimalCost, double inputEpsilon) {
		// a solution consider as W-optimal iff:
		// h*(n)(1+epsilon) < U
		
		logger.debug("h*(n)[" + optimalCost +"](1+epsilon)[" + (1+inputEpsilon) +"] < U[" +U+"]");

		return  U<= optimalCost * (1+inputEpsilon);
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
