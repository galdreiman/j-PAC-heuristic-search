package org.cs4j.core.pac.conf;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;

/**
 * Created by Gal Dreiman on 30/06/2017.
 */

@Config.Sources({ "file:./conf/pac.config",
                  "classpath:pac.config" })
public interface PacConfig extends Config {

    PacConfig instance = ConfigFactory.create(PacConfig.class);

    String PAC = "pac";

    //-----------------------------------------
    // statistics generator
    //-----------------------------------------
    String STATS_GEN= ".statisticsGenerator";
    @Key(PAC + STATS_GEN +".numInstances")
    @DefaultValue("50000")
    int pacPreprocessNumInstances();

    @Key(PAC + STATS_GEN +".domains")
    @DefaultValue("VacuumRobot")
    @ConverterClass(ClassConverter.class)
    Class[] pacDomains();

    @Key(PAC + STATS_GEN +".gridMinDistLow")
    @DefaultValue("66")
    double gridMinDistLow();

    @Key(PAC + STATS_GEN +".gridMinDistHigh")
    @DefaultValue("66")
    double gridMinDistHigh();

    @Key(PAC + STATS_GEN +".gridMinDistDelta")
    @DefaultValue("5")
    double gridMinDistDelta();

    @Key(PAC + STATS_GEN +".gridMinDistTest")
    @DefaultValue("66")
    double gridMinDistTest();


    //-----------------------------------------
    // PAC Preprocess
    //-----------------------------------------
    String PREPROCESS = ".preprocess";
    @Key(PAC + PREPROCESS +".inputEpsilons")
    @Separator(",")
    @DefaultValue("0.0,0.05,0.1,0.2,0.3")
    double[] inputPreprocessEpsilons();

    @Key(PAC + PREPROCESS +".numInstances")
    @DefaultValue("50000")
    int numInstances();

    @Key(PAC + PREPROCESS +".trainRatio")
    @DefaultValue("0.9")
    double trainRatio();

    @Key(PAC + PREPROCESS +".domains")
    @DefaultValue("Pancakes")
    @ConverterClass(ClassConverter.class)
    Class[] pacPreProcessDomains();

    @Key(PAC + PREPROCESS +".useResampleFilter")
    @DefaultValue("true")
    boolean pacPreProcessUseResampleFilter();

    @Key(PAC + PREPROCESS +".biasToUniformClass")
    @DefaultValue("0.7")
    double pacPreProcessBiasToUniformClass();

    @Key(PAC + PREPROCESS +".useDomainFeatures")
    @DefaultValue("false")
    boolean useDomainFeatures();

    //-----------------------------------------
    // PAC online search
    //-----------------------------------------
    String ONLINE = ".online";
    @Key(PAC + ONLINE +".inputEpsilons")
    @Separator(",")
    @DefaultValue("0.0,0.05,0.1,0.2,0.3")
    double[] inputOnlineEpsilons();

    @Key(PAC + ONLINE +".inputDeltas")
    @Separator(",")
    @DefaultValue("0.0,0.1,0.2,0.3")
    double[] inputOnlineDeltas();

    @Key(PAC + ONLINE +".domains")
    @DefaultValue("VacuumRobot,GridPathFinding")
    @ConverterClass(ClassConverter.class)
    Class[] onlineDomains();

    @Key(PAC + ONLINE +".pacConditions")
    @DefaultValue("ML-NN,ML-J48,RatioBased,f-min")
    @ConverterClass(PacConditionConverter.class)
    Class[] onlinePacConditions();

    //-----------------------------------------
    // ML PAC for hard domains
    //-----------------------------------------
    String PREDICTION = ".prediction";

    @Key(PAC + PREDICTION +".numInstances")
    @DefaultValue("100")
    int PredictionNumInstances();

    @Key(PAC + PREDICTION +".inputEpsilons")
    @Separator(",")
    @DefaultValue("0.0,0.05,0.1,0.2,0.3")
    double[] inputPredictionEpsilons();

    @Key(PAC + PREDICTION +".inputDeltas")
    @Separator(",")
    @DefaultValue("0.0,0.1,0.2,0.3,0.8")
    double[] inputPredictionDeltas();

    @Key(PAC + PREDICTION +".domainsAndExpValues")
    @DefaultValue("GridPathFinding:80:84:2:85,Pancakes:15:17:1:18,VacuumRobot:4:6:1:7,DockyardRobot:4:6:1:7")
    @ConverterClass(ExperimentConverter.class)
    MLPacPreprocessExperimentValues[] predictionDomainsAndExpValues();

    @Key(PAC + PREDICTION +".applyEvaluation")
    @DefaultValue("false")
    boolean PredictionApplyEvaluation();

    @Key(PAC + PREDICTION +".outputTestRawFeatures")
    @DefaultValue("false")
    boolean outputTestRawFeatures();

    @Key(PAC + PREDICTION +".domainLevelsForRatioBased")
    @DefaultValue("56,58,60")
    int[] PredictiondomainLevelsForRatioBased();

    @Key(PAC + PREDICTION +".domainLevelTestForRatioBased")
    @DefaultValue("60")
    int PredictiondomainLevelTestForRatioBased();

    @Key(PAC + PREDICTION +".trainUntilFMin")
    @DefaultValue("false")
    boolean trainUntilFMin();

}
