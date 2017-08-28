# j-PAC-heuristic-search

install oracle java 8 ubuntu
-----------------------------

$ sudo add-apt-repository ppa:webupd8team/java
$ sudo apt-get update
$ sudo apt-get install oracle-java8-installer

install gradle 4.0
-------------------
1- Installing SDKMAN:
	$ curl -s "https://get.sdkman.io" | bash
	$ source "$HOME/.sdkman/bin/sdkman-init.sh"
	$ sdk version
		> sdkman 5.0.0+51
 
2- $ sdk install gradle 4.1

install git
------------
$ sudu apt-get install git

build a shadow jar
------------------
$ gradle shadow



Run ML PAC experiment
---------------------
java -cp build/libs/pac-search-1.0-all.jar org.cs4j.core.generators.PancakesGenerator 30 30 


java -cp build/libs/pac-search-1.0-all.jar org.cs4j.core.algorithms.pac.preprocess.StatisticsGenerator && echo "donse statistics for 50K" > done_static.log && java -cp build/libs/pac-search-1.0-all.jar org.cs4j.core.algorithms.pac.preprocess.MLPacPreprocess && echo "donse statistics for 50K" > done_PAC_preprocess.log && java -cp build/libs/pac-search-1.0-all.jar org.cs4j.core.experiments.MLPacExperiment