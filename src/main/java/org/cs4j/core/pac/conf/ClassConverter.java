package org.cs4j.core.pac.conf;

import org.aeonbits.owner.Converter;
import org.cs4j.core.domains.*;

import java.lang.reflect.Method;

/**
 * Created by Gal Dreiman on 30/06/2017.
 */



public class ClassConverter implements Converter<Class> {
    @Override
    public Class convert(Method method, String input) {

        switch (input){
            case "Pancakes":
                return Pancakes.class;
            case "GridPathFinding":
                return GridPathFinding.class;
            case "FifteenPuzzle":
                return FifteenPuzzle.class;
            case "RawGraph":
                return RawGraph.class;
            case "VacuumRobot":
            default:
                return VacuumRobot.class;

        }

    }
}
