# ClosedDiversity

Implementation of the **ClosedDiversity** pattern sampler.

For running the code , the minimum command syntax is :
JAVA -JAR PROGRAMM CONSTRAINT datasetPath resultsPatternsFilePath resultsSummaryFilePath -f absoluteFrequency OPTIONS 
CONSTRAINT = 
	CP : ClosedPattern
	CD : ClosedDiversity
	CPJ : ClosedPattern + Jaccard Consistancy evaluation
	CDJ : ClosedDiversity + Jaccard Consistancy evaluation

datasetPath : dataset file path
resultsPatternsFilePath : result itemsets file path
resultsSummaryFilePath : file for some logs data

OPTIONS = 
	-f [num]: the frequency absolute value (between 0 and total number of transactions)
	-j [threshold]: the diversity threshold (float between 0 and 1)
	-s [strategy] [witness strategy]: the search strategy [MINCOV NONE, WITNESS WITNESSFIRSTSOL, WITNESS WITNESSDIVSOL]
	-th [num]: the number of threads to use for jaccard evaluation 

The second, third and fourth arguments must be set for the dataset, the itemsets file and the logs summary file 


############################################################


For a detailed description, consult 
the following [publication](https://hal-genes.archives-ouvertes.fr/UNICAEN/hal-02935080):

A. Hien, S. Loudni, N. Aribi, Y. Lebbah, M. Laghzaoui, A. Ouali, A. Zimmermann (2020)
*A Relaxation-based Approach for Mining Diverse Closed Patterns *
