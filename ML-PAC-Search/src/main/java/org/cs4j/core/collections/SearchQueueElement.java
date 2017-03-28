/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cs4j.core.collections;

/**
 * The SearchQueueElement interface (see intrusive data structures)
 * 
 * @author Matthew Hatem
 */
public interface SearchQueueElement {

    /**
    * Sets the indexes of this object in the data structure.
    *
    * @param key
    * @param index
    */
    public void setIndex(int key, int index);

    /**
    * Returns the index of this object in the data structure.
    *
    * @return
    */
    public int getIndex(int key);

    /**
    *
    * @return Elements F value;
    */
    public double getF();

    /**
    *
    * @return Elements G value;
    */
    public double getG();

    /**
     *
     * @return Elements Depth value;
     */
    public double getDepth();

    /**
    *
    * @return Elements H value;
    */
    public double getH();

    /**
    *
    * @return Elements D value;
    */
    public double getD();

    /**
     *
     * @return Elements Hhat value;
     */
    public double getHhat();

    /**
     *
     * @return Elements Dhat value;
     */
    public double getDhat();

    /**
     *
     * @return Elements parent;
     */
    SearchQueueElement getParent();
}
