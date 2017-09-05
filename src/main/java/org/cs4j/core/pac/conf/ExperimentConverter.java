package org.cs4j.core.pac.conf;

import org.aeonbits.owner.Converter;
import org.cs4j.core.domains.*;
import sun.security.jca.GetInstance;

import java.lang.reflect.Method;

/**
 * Created by Gal Dreiman on 19/08/2017.
 */
public class ExperimentConverter implements Converter<MLPacPreprocessExperimentValues> {


    @Override
    public MLPacPreprocessExperimentValues convert(Method method, String input) {

        String[] parts = input.split(":");

        if(parts.length != 5){
            throw new MLPacPreprocessExperimentValuesException("Not enough parts in experiment values. Got: "+ input);
        }

        Class domainClass = getDomainClass(parts[0]);
        int trainLevelLow = Integer.parseInt(parts[1]);
        int trainLevelHigh = Integer.parseInt(parts[2]);
        int trainLevelDelta = Integer.parseInt(parts[3]);
        int testLevel = Integer.parseInt(parts[4]);

        return new MLPacPreprocessExperimentValues(domainClass,trainLevelLow,trainLevelHigh,trainLevelDelta,testLevel);
    }

    private Class getDomainClass(String className) {

        switch (className){
            case "Pancakes":
                return Pancakes.class;
            case "GridPathFinding":
                return GridPathFinding.class;
            case "FifteenPuzzle":
                return FifteenPuzzle.class;
            case "RawGraph":
                return RawGraph.class;
            case "DockyardRobot":
                return DockyardRobot.class;
            case "VacuumRobot":
            default:
                return VacuumRobot.class;

        }
    }

    public static void main(String[] args){
        MLPacPreprocessExperimentValues[] a = PacConfig.instance.predictionDomainsAndExpValues();
        for(MLPacPreprocessExperimentValues x : a){
            System.out.println(x);
        }
    }

    public static class MLPacPreprocessExperimentValuesException extends RuntimeException{

        public MLPacPreprocessExperimentValuesException(String s){
            super(s);
        }
    }
}
