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
package org.cs4j.core.test.algorithms;

import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchDomain;
import org.cs4j.core.algorithms.*;
import org.junit.Test;

import java.io.FileNotFoundException;

public class TestAllBasics {
		
	@Test
	public void testAstarBinHeap() throws FileNotFoundException {
		SearchDomain domain = TestUtils.createFifteenPuzzle("12");
		SearchAlgorithm algo = new WAStar();
		TestUtils.checkSearchAlgorithm(domain, algo, 65271, 32470, 45);
	}	

	@Test
	public void testRBFS() throws FileNotFoundException {
		SearchDomain domain = TestUtils.createFifteenPuzzle("12");
		SearchAlgorithm algo = new RBFS();
		TestUtils.checkSearchAlgorithm(domain, algo, 301098, 148421, 45);
	}	
	
	@Test
	public void testIDAstar() throws FileNotFoundException {
		SearchDomain domain = TestUtils.createFifteenPuzzle("12");
		SearchAlgorithm algo = new IDAstar();
		TestUtils.checkSearchAlgorithm(domain, algo, 546343, 269708, 45);
	}		

	@Test
	public void testEES() throws FileNotFoundException {
		SearchDomain domain = TestUtils.createFifteenPuzzle("12");
		SearchAlgorithm algo = new EES(2);
		TestUtils.checkSearchAlgorithm(domain, algo, 5131, 2506, 55);
	}	
	
	@Test
	public void testWRBFS() throws FileNotFoundException {
		SearchDomain domain = TestUtils.createFifteenPuzzle("12");
		SearchAlgorithm algo = new WRBFS();
		TestUtils.checkSearchAlgorithm(domain, algo, 301098, 148421, 45);
	}	
	

	

	
	public static void main(String[] args) throws FileNotFoundException {
		TestAllBasics test = new TestAllBasics();
		test.testEES();
	}

}
