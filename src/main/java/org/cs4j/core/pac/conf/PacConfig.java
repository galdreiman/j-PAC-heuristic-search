package org.cs4j.core.pac.conf;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;

/**
 * Created by Gal Dreiman on 30/06/2017.
 */

@Config.Sources({ "file:~/pac.config",
        "file:${user.dir}/../conf/pac.config",
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
    @DefaultValue("0.0,0.1,0.2,0.3,0.8")
    double[] inputOnlineDeltas();

    @Key(PAC + ONLINE +".domains")
    @DefaultValue("VacuumRobot,GridPathFinding")
    @ConverterClass(ClassConverter.class)
    Class[] onlineDomains();

    @Key(PAC + ONLINE +".pacConditions")
    @DefaultValue("ML-NN,ML-J48,RatioBased")
    @ConverterClass(PacConditionConverter.class)
    Class[] onlinePacConditions();



}
