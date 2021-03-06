package org.cs4j.core;

import org.apache.log4j.Logger;
import org.cs4j.core.pac.conf.PacConfig;
import weka.core.Attribute;

import java.util.*;

public class MLPacFeatureExtractor {

	private final static Logger logger = Logger.getLogger(MLPacFeatureExtractor.class);

	public static String getFeaturesARFFHeaderForPredictedStats(boolean withTarget) {
		StringBuilder b = new StringBuilder();
		b.append("% Title: PAC MachineLearning dataset \n");
		b.append("% \n");
		b.append("% \n");

		b.append("@RELATION ML-PAC-STATISTICS-PREDICTOR");
		b.append("\n");
		b.append("\n");

		b.append("@ATTRIBUTE initialH  NUMERIC\n");
		b.append("@ATTRIBUTE DomainLevel  NUMERIC\n");

		for(int i =0; i < 3; i++) {
			b.append(String.format("@ATTRIBUTE childH-%d  NUMERIC\n", i));
			b.append(String.format("@ATTRIBUTE childG-%d  NUMERIC\n", i));
			b.append(String.format("@ATTRIBUTE childDepth-%d  NUMERIC\n", i));

			for(int j =0; j < 3; j++) {
				b.append(String.format("@ATTRIBUTE grandchildH-%d-%d  NUMERIC\n", i,j));
				b.append(String.format("@ATTRIBUTE grandchildG-%d-%d  NUMERIC\n", i,j));
				b.append(String.format("@ATTRIBUTE grandchildDepth-%d-%d  NUMERIC\n", i,j));
			}
		}

		if(withTarget) {
			b.append("@ATTRIBUTE opt-cost  NUMERIC");
		}

		if(PacConfig.instance.useDomainFeatures()) {
			b.append("@ATTRIBUTE remainingDirtyLocationCount_start  NUMERIC\n");
			b.append("@ATTRIBUTE dirtyVector_start  NUMERIC\n");
			b.append("@ATTRIBUTE remainingDirtyLocationCount_goal  NUMERIC\n");
			b.append("@ATTRIBUTE dirtyVector_goal  NUMERIC\n");
		}
		b.append("\n");
		b.append("\n");
		b.append("@DATA");
		b.append("\n");


		return b.toString();
	}

	public static String getFeaturesARFFHeaderForPredictedBoundedSol(boolean withTarget) {
		StringBuilder b = new StringBuilder();
		b.append("% Title: PAC MachineLearning dataset \n");
		b.append("% \n");
		b.append("% \n");

		b.append("@RELATION ML-PAC-STATISTICS-PREDICTOR");
		b.append("\n");
		b.append("\n");

		b.append("@ATTRIBUTE initialH  NUMERIC\n");
		b.append("@ATTRIBUTE DomainLevel  NUMERIC\n");

		for(int i =0; i < 3; i++) {
			b.append(String.format("@ATTRIBUTE childH-%d  NUMERIC\n", i));
			b.append(String.format("@ATTRIBUTE childG-%d  NUMERIC\n", i));
			b.append(String.format("@ATTRIBUTE childDepth-%d  NUMERIC\n", i));

			for(int j =0; j < 3; j++) {
				b.append(String.format("@ATTRIBUTE grandchildH-%d-%d  NUMERIC\n", i,j));
				b.append(String.format("@ATTRIBUTE grandchildG-%d-%d  NUMERIC\n", i,j));
				b.append(String.format("@ATTRIBUTE grandchildDepth-%d-%d  NUMERIC\n", i,j));
			}
		}
		if(PacConfig.instance.useDomainFeatures()) {
			b.append("@ATTRIBUTE remainingDirtyLocationCount_start  NUMERIC\n");
			b.append("@ATTRIBUTE dirtyVector_start  NUMERIC\n");
			b.append("@ATTRIBUTE remainingDirtyLocationCount_goal  NUMERIC\n");
			b.append("@ATTRIBUTE dirtyVector_goal  NUMERIC\n");
		}

		if(withTarget) {
			b.append("@ATTRIBUTE is-w-opt  {true,false}");
		}



		b.append("\n");
		b.append("\n");
		b.append("@DATA");
		b.append("\n");


		return b.toString();
	}

	public static String getFeaturesCSVHeaderForPredictedStats(boolean withTarget) {
		StringBuilder b = new StringBuilder();

		b.append("initialH,");
		b.append("DomainLevel,");

		for(int i =0; i < 3; i++) {
			b.append(String.format("childH-%d  NUMERIC,", i));
			b.append(String.format("childG-%d  NUMERIC,", i));
			b.append(String.format("childDepth-%d  NUMERIC,", i));

			for(int j =0; j < 3; j++) {
				b.append(String.format("grandchildH-%d-%d,", i,j));
				b.append(String.format("grandchildG-%d-%d,", i,j));
				b.append(String.format("grandchildDepth-%d-%d,", i,j));
			}
		}

		if(PacConfig.instance.useDomainFeatures()) {
			b.append("@ATTRIBUTE remainingDirtyLocationCount_start  NUMERIC\n");
			b.append("@ATTRIBUTE dirtyVector_start  NUMERIC\n");
			b.append("@ATTRIBUTE remainingDirtyLocationCount_goal  NUMERIC\n");
			b.append("@ATTRIBUTE dirtyVector_goal  NUMERIC\n");
		}
		if(withTarget) {
			b.append("opt-cost");
		}

		return b.toString();
	}



	public enum PacFeature{
		GENERATED, EXPANDED, ROPENED, COST, LENGTH, G_0,H_0,G_1,H_1,G_2,H_2,W,
		remainingDirtyLocationsCount,
		dirtyVector,
		IS_W_OPT;
		
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
		if(PacConfig.instance.useDomainFeatures()) {
			return "generated,expanded,reopened,cost,g1,h1,g2,h2,g3,h3,w," +
					"remainingDirtyLocationCount_start,dirtyVector_start," +
					"remainingDirtyLocationCount_goal,dirtyVector_goal," +
					"is-W-opt";
		} else{
			return "generated,expanded,reopened,cost,g1,h1,g2,h2,g3,h3,w,is-W-opt";
		}
	}

	public static String getFeaturesARFFHeader(){
		StringBuilder b = new StringBuilder();
		b.append("% Title: PAC MachineLearning dataset \n");
		b.append("% \n");
		b.append("% \n");

		b.append("@RELATION ML-PAC");
		b.append("\n");
		b.append("\n");
		b.append("@ATTRIBUTE generated  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE expanded  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE reopened  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE cost  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE g1  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE h1  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE g2  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE h2  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE g3  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE h3  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE w  NUMERIC");
		b.append("\n");
		if(PacConfig.instance.useDomainFeatures()) {
			b.append("@ATTRIBUTE remainingDirtyLocationCount_start  NUMERIC");
			b.append("\n");
			b.append("@ATTRIBUTE dirtyVector_start  NUMERIC");
			b.append("\n");
			b.append("@ATTRIBUTE remainingDirtyLocationCount_goal  NUMERIC");
			b.append("\n");
			b.append("@ATTRIBUTE dirtyVector_goal  NUMERIC");
			b.append("\n");
		}
		b.append("@ATTRIBUTE is-W-opt  {true,false}");
		b.append("\n");
		b.append("\n");
		b.append("@DATA");
		b.append("\n");
//		return "generated,expanded,reopened,cost,g1,h1,g2,h2,g3,h3,w,is-W-opt";
		return b.toString();
	}

	public static String getFeaturesARFFHeaderBoundSolPred(){
		StringBuilder b = new StringBuilder();
		b.append("% Title: PAC MachineLearning dataset \n");
		b.append("% \n");
		b.append("% \n");

		b.append("@RELATION ML-PAC");
		b.append("\n");
		b.append("\n");
		b.append("@ATTRIBUTE generated  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE expanded  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE reopened  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE domainLevel  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE cost  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE g1  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE h1  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE h1ToLevel  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE g2  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE h2  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE h2ToLevel  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE g3  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE h3  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE h3ToLevel  NUMERIC");
		b.append("\n");
		b.append("@ATTRIBUTE w  NUMERIC");
		b.append("\n");
		if(PacConfig.instance.useDomainFeatures()) {
			b.append("@ATTRIBUTE remainingDirtyLocationCount_start  NUMERIC");
			b.append("\n");
			b.append("@ATTRIBUTE dirtyVector_start  NUMERIC");
			b.append("\n");
			b.append("@ATTRIBUTE remainingDirtyLocationCount_goal  NUMERIC");
			b.append("\n");
			b.append("@ATTRIBUTE dirtyVector_goal  NUMERIC");
			b.append("\n");
		}
		b.append("@ATTRIBUTE is-W-opt  {true,false}");
		b.append("\n");
		b.append("\n");
		b.append("@DATA");
		b.append("\n");
//		return "generated,expanded,reopened,cost,g1,h1,g2,h2,g3,h3,w,is-W-opt";
		return b.toString();
	}

	public static String getFeaturesCsvHeader(){
		StringBuilder b = new StringBuilder();

		b.append("generated ");
		b.append(",");
		b.append("expanded");
		b.append(",");
		b.append("reopened");
		b.append(",");
		b.append("cost");
		b.append(",");
		b.append("g1");
		b.append(",");
		b.append("h1");
		b.append(",");
		b.append("g2 ");
		b.append(",");
		b.append("h2 ");
		b.append(",");
		b.append("g3 ");
		b.append(",");
		b.append("h3 ");
		b.append(",");
		b.append("w ");
		b.append(",");
		if(PacConfig.instance.useDomainFeatures()) {
			b.append("remainingDirtyLocationCount_start");
			b.append(",");
			b.append("dirtyVector_start");
			b.append(",");
			b.append("remainingDirtyLocationCount_goal");
			b.append(",");
			b.append("dirtyVector_goal");
			b.append(",");
		}
		b.append("is-W-opt");

		b.append("\n");
//		return "generated,expanded,reopened,cost,g1,h1,g2,h2,g3,h3,w,is-W-opt";
		return b.toString();
	}

	public static String getFeaturesCsvHeaderBoundSolPred() {
		StringBuilder b = new StringBuilder();

		b.append("generated ");
		b.append(",");
		b.append("expanded");
		b.append(",");
		b.append("reopened");
		b.append(",");
		b.append("domainLevel");
		b.append(",");
		b.append("cost");
		b.append(",");
		b.append("g1");
		b.append(",");
		b.append("h1");
		b.append(",");
		b.append("h1ToLevel");
		b.append(",");
		b.append("g2 ");
		b.append(",");
		b.append("h2 ");
		b.append(",");
		b.append("h2ToLevel");
		b.append(",");
		b.append("g3 ");
		b.append(",");
		b.append("h3 ");
		b.append(",");
		b.append("h3ToLevel");
		b.append(",");
		b.append("w ");
		b.append(",");
		if (PacConfig.instance.useDomainFeatures()) {

				b.append("remainingDirtyLocationCount_start");
				b.append(",");
				b.append("dirtyVector_start");
				b.append(",");
				b.append("remainingDirtyLocationCount_goal");
				b.append(",");
				b.append("dirtyVector_goal");
				b.append(",");
			}
		b.append("is-W-opt");

		b.append("\n");
//		return "generated,expanded,reopened,cost,g1,h1,g2,h2,g3,h3,w,is-W-opt";
			return b.toString();
	}


	public static Map<PacFeature,Double> extractFeaturesFromSearchResultIncludeTarget(SearchResult result, double optimalCost, double inputEpsilon){
		logger.debug("MLPacFeatureExtractor:extractFeaturesFromSearchResult");
		Map<PacFeature,Double> features = extractFeaturesFromSearchResult(result);
		
		Double U = result.getBestSolution().getCost();

		features.put(PacFeature.IS_W_OPT, new Double(isWOpttimal(U, optimalCost, inputEpsilon)==true? 1.0 : 0.0));
		
		return features;
	}
	
	public static Map<PacFeature,Double> extractFeaturesFromSearchResult(SearchResult b){
		Map<PacFeature,Double> features = new HashMap<>();

		// "generated,expanded,reopened,cost,g1,h1,g2,h2,g3,h3,w,is-W-opt"

		features.put(PacFeature.GENERATED, new Double(b.getGenerated()));
		features.put(PacFeature.EXPANDED, new Double(b.getExpanded()));
		features.put(PacFeature.ROPENED, new Double(b.getReopened()));

		Double U = b.getBestSolution().getCost();
		features.put(PacFeature.COST, new Double(U));

		// Get h and g values of the first nodes on the found path
		SearchResult.Solution solution = b.getBestSolution();
		double g=0.0;
		SearchDomain.State parent = null;
		SearchDomain.State current;
		double h;
		int maxPrefix = 3;
		int i=0;
		for(;i<solution.getLength()&& i<maxPrefix;i++){
			current = solution.getStates().get(0);
			if(parent!=null){
				try {
					g += solution.getOperators().get(i).getCost(current, parent);
				}catch (Exception e) {
					logger.error("Failed to calculate g properly. Exception: "+ e.getMessage());
					break;
				}
			}
			h=current.getH();

			features.put(PacFeature.getPacFeature("h_" + i), h);
			features.put(PacFeature.getPacFeature("g_" + i), g);

			parent = current;
		}
		while(i<maxPrefix){
			features.put(PacFeature.getPacFeature("h_" + i), -1.0);
			features.put(PacFeature.getPacFeature("g_" + i), -1.0);
			i++;
		}

		features.put(PacFeature.W, 1.0 + (Double) b.getExtras().get("epsilon"));

		return features;
	}

	public static String getHeaderLineFeatures() {
		List<String> attributes = null;
		if(PacConfig.instance.useDomainFeatures()){
			attributes = Arrays.asList("domain", "instance", "index", "generated", "expanded", "reopened", "cost", "g1",
					"h1","g2", "h2", "g3","h3","oprimal_cost", "w",
					"remainingDirtyLocationCount_start",
					"dirtyVector_start",
					"remainingDirtyLocationCount_goal",
					"dirtyVector_goal",
					"is-W-opt" );
		} else{
			attributes = Arrays.asList("domain", "instance", "index", "generated", "expanded", "reopened", "cost", "g1",
					"h1","g2", "h2", "g3","h3","oprimal_cost", "w",
					"is-W-opt" );
		}
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
