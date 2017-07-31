package org.cs4j.core.pac.evaluation;


import weka.core.Attribute;
import weka.core.NominalAttributeInfo;

import java.util.List;

/**
 * Created by Gal Dreiman on 31/07/2017.
 */
public class PacAttribute extends Attribute{

    public PacAttribute(String attributeName, int index, boolean isStringAttr){
        super(attributeName,index);
        this.m_Type = NOMINAL;

        if (isStringAttr) {
            m_AttributeInfo = new NominalAttributeInfo(null, attributeName);
            m_Type = STRING;
        }
    }

    public PacAttribute(String attributeNAme){
        super((attributeNAme));
    }
}
