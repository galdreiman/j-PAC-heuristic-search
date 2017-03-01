package org.cs4j.core.experiments;

import org.apache.log4j.Logger;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.IDAstar;
import org.cs4j.core.domains.DockyardRobot;
import org.cs4j.core.domains.FifteenPuzzle;
import org.cs4j.core.domains.Pancakes;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Roni Stern on 26/02/2017.
 *
 * This class holds helper functions that are useful for running experiments
 */
public class ExperimentUtils {
    final static Logger logger = Logger.getLogger(ExperimentUtils.class);

    /**
     * Create a constructor for the search domain, to enable creating search domains for a
     * @param domainClass the class of the domain
     */
    public static Constructor<?> getSearchDomainConstructor(Class domainClass) {
        Constructor<?> cons = null;
        try {
            cons = domainClass.getConstructor(InputStream.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return cons;
    }


    /**
     * Read and setup a search domain
     * @param inputPath Location of the search domain
     * @param domainParams Additional parameters
     * @param cons Constructor of the search domain
     * @param i index of problem instance
     * @return A SearchDomain object
     *
     */
    public static SearchDomain getSearchDomain(String inputPath, Map<String, String> domainParams, Constructor<?> cons, int i){
        InputStream is = null;
        SearchDomain domain=null;
        try {
            is = new FileInputStream(new File(inputPath + "/" + i + ".in" ));
            domain = (SearchDomain) cons.newInstance(is);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        // Set additional parameters to the domain
        for(Map.Entry<String, String> entry : domainParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            domain.setAdditionalParameter(key,value);
        }
        return domain;
    }


    public static SearchDomain getSearchDomain(Class domainClass, int instance)
    {
        return getSearchDomain(domainClass, instance, RunType.TRAIN); // defaul implementation - for backward competability
    }
    
    public static SearchDomain getSearchDomain(Class domainClass, int instance, RunType rt)
    {
        String inputPath = DomainExperimentData.get(domainClass,rt).inputPath;
        Constructor constructor = ExperimentUtils.getSearchDomainConstructor(domainClass);
        return ExperimentUtils.getSearchDomain(inputPath,new HashMap<>(),constructor,instance);
    }

    /**
     * Returns the class names from the command line args. @TODO: Replace all this with a descent framework
     * for parsing command line arguments (there are many such frameworks)
     * @param args
     * @return
     */
    public static Class[] readClasses(String[] args){
        // Default classes
        boolean classesFound=false;
        int i;
        for(i=0;i<args.length;i++){
            if(args[i].equals("-classes")) {
                classesFound=true;
                break;
            }
        }
        if(classesFound==false)
            return new Class[]{DockyardRobot.class, Pancakes.class,FifteenPuzzle.class};

        List<Class> domainsList = new ArrayList<>();
        Class domainClass=null;
        for(int j=i+1;j<args.length;j++){
            if(args[j].startsWith("-")) { // Next type of parameter
                return domainsList.toArray(new Class[]{});
            }
            else{
                try {
                    domainClass=Class.forName(args[j]);
                } catch (ClassNotFoundException e) {
                    logger.error("Class "+args[j] + " unknown");
                    domainClass=null;
                }
                if(domainClass!=null)
                    domainsList.add(domainClass);
            }
        }
        return domainsList.toArray(new Class[]{});
    }
}
