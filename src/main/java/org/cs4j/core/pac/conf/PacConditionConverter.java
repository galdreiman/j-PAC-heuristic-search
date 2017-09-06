package org.cs4j.core.pac.conf;

import org.aeonbits.owner.Converter;
import org.cs4j.core.algorithms.pac.FMinCondition;
import org.cs4j.core.algorithms.pac.conditions.*;

import java.lang.reflect.Method;

/**
 * Created by user on 30/06/2017.
 */
public class PacConditionConverter implements Converter<Class> {
    @Override
    public Class convert(Method method, String input) {
        switch (input){
            case "ML-NN":
                return MLPacConditionNN.class;
            case "ML-J48":
                return MLPacConditionJ48.class;
            case "RatioBased":
                return RatioBasedPACCondition.class;
            case "f-min":
                return FMinCondition.class;
            case "OpenBased":
                return OpenBasedPACCondition.class;
            case "Trivial":
            default:
                return TrivialPACCondition.class;
        }
    }
}
