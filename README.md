# ClosedDiversity

This repository includes two main patterns mining tools with some extensions added : **ClosedPatterns** and **ClosedDiversity** global contraints.  

Both tools have been implemented in java and use [@choco-solver](https://github.com/chocoteam/choco-solver) library to model and solve some pattern mining problems.

Current stable version is 1.0.1 (15 Jul 2021).

For running the code after building the source code, the minimum command syntax is :

**JAVA -JAR PROGRAMM CONSTRAINT [datasetFilePath] [resultsPatternsFilePath] [resultsSummaryFilePath] -f absoluteFrequency OPTIONS**

CONSTRAINT = 

	CP : ClosedPattern
	CD : ClosedDiversity
	CPD : ClosedPattern + Diversity Constraint
	CPJ : ClosedPattern + Jaccard Consistancy evaluation
	CDJ : ClosedDiversity + Jaccard Consistancy evaluation

Files =

	[datasetFilePath] : dataset file path (uci datasets format)
	[resultsPatternsFilePath] : result itemsets file path
	[resultsSummaryFilePath] : file for some logs data

OPTIONS = 
	
	-f [num]: the frequency absolute value (between 1 and total number of transactions)
	-j [threshold]: the diversity threshold (float between 0 and 1)
	-s [strategy] [witness strategy]: the search strategy [MINCOV NONE, WITNESS WITNESSFIRSTSOL, WITNESS WITNESSDIVSOL]
	-th [num]: the number of threads to use for jaccard evaluation 

The second, third and fourth arguments must be set for the dataset, the itemsets file and the logs summary file.


**EXAMPLES :**
	
	java -jar PROGRAM.JAR CP mushroom.dat mushroom.res mushromm.ana -f 400
	java -jar PROGRAM.JAR CPD hepatitis.dat hepatitis.res hepatitis.ana -f 28 -j 0.05
	java -jar PROGRAM.JAR CD retail.dat retail.res retail.ana -f 4409 -j 0.5 MINCOV NONE
	java -jar PROGRAM.JAR CD connect.dat connect.res connect.ana -f 2500 -j 0.35 WITNESS WITNESSFIRSTSOL


## Documentations

For a detailed description, consult 
the following [publication](https://normandie-univ.hal.science/hal-03244005v1):

A. Hien, S. Loudni, N. Aribi, Y. Lebbah, M. Laghzaoui, A. Ouali, A. Zimmermann (2020), 
*A Relaxation-based Approach for Mining Diverse Closed Patterns*
