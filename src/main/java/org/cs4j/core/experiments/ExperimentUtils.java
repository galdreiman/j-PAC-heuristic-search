package org.cs4j.core.experiments;

import org.cs4j.core.SearchDomain;
import org.cs4j.core.mains.DomainExperimentData;
import org.cs4j.core.mains.DomainExperimentData.RunType;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roni Stern on 26/02/2017.
 *
 * This class holds helper functions that are useful for running experiments
 */
public class ExperimentUtils {

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

}
