package fr.unicaen.mining;

import static java.lang.System.out;
import static org.chocosolver.solver.search.strategy.Search.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
//import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
//import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.variables.BoolVar;
//import org.chocosolver.solver.variables.IntVar;

import fr.unicaen.mining.datasets.DataSet;
import fr.unicaen.mining.models.AbstractProblem;
import fr.unicaen.mining.models.closedpatterns.globalconstraints.ClosedPatterns;
import fr.unicaen.mining.models.closedpatterns.globalconstraints.ClosedPatterns_with_Transactions;
import fr.unicaen.mining.models.closeddiversity.ClosedDiversity;
//import fr.unicaen.mining.models.xor.ConstraintXOR;
import fr.unicaen.mining.structures.History;
import fr.unicaen.mining.structures.Solution;
import fr.unicaen.mining.structures.TItemSet;
//import fr.unicaen.mining.structures.TTransactionSet;
import fr.unicaen.mining.util.ClosedDiversityStrategies;
import fr.unicaen.mining.util.MemoryLogger;


public class RunMain extends AbstractProblem {
	static String[] argparam;
	public BoolVar[] p;
	public BoolVar[] T;
	public Constraint c;
	public ClosedDiversity cd;
	public ClosedPatterns cp;
	public ClosedPatterns_with_Transactions cpd;
	
	int minFreq;
	double jmax;
	public History history;
	
	public DataSet dataset;
	public String pathInput;
	public String pathOutput;
	public String pathAnalyse;

	public int k;
	public int nbSolutions;
	
	public int nbThread;
	public String searchStrategy = "";
	public static String witnessStrategy = "";
	
	public static int witnessItem = -1;
	public static int currentItem = -1;
	public static boolean isWitness = false;
	
	public int nbWitnessNode = 0;
	public int nbTotalWitnessSolutions = 0;
	public int nbTotalSolutionsWitnessNode = 0;
	public Solution[] witnessNodeSolutions;
	
	public BufferedWriter writer;
	
	private String method = "";
	private boolean continueJaccard = true;
	private int nbSolutionsFilteredByJaccard;
	
	
	public void buildModel() {

		model = new Model("ClosedDiversity");
		try {
			
			// getting the data
			pathInput = argparam[0]; // input file
			pathOutput = argparam[1]; // output file
			pathAnalyse = argparam[2]; // output summary file
			
			// dataset initialization
			dataset = new DataSet(pathInput);
			p = model.boolVarArray("item", dataset.getNbrVar());
			
			// frequency threshold
			minFreq = Integer.valueOf(argparam[3]);
			
			// Filtering all non frequent items (i.e. cover(item) < theta)
			for (int item = 0; item < dataset.getNbrVar(); item++) {
				if (dataset.getVerticalDataBase()[item].cardinality() < minFreq)
					p[item].setToFalse(Cause.Null);
			}
			
			history = new History();
			
			
			if(argparam.length == 6) {
				if(argparam[4].equals("CP")) { // CP
					method = argparam[4]; 
					
					cp = new ClosedPatterns(dataset, minFreq, p);
					c = new Constraint("Closed Patterns", cp);
				}
				else { // CPD
					method = argparam[5];
					T = model.boolVarArray("transactions", dataset.getTransactionsSize());
					
					// diversity threshold
					jmax = Double.valueOf(argparam[4]);
					
					cpd = new ClosedPatterns_with_Transactions(dataset, minFreq, p, T);
					c = new Constraint("Closed Patterns ++ Diversity Constraint", cpd);
					//model.post(c);
				}
				
			}
			else if(argparam.length == 7) {
				method = argparam[5]; // CPJ
				
				// diversity threshold
				jmax = Double.valueOf(argparam[4]);
				// number of thread
				try {
					nbThread = Integer.parseInt(argparam[6]);
				}catch (NumberFormatException e) {
					System.out.println("Error: the number of thread should be an integer.");
					e.printStackTrace();
					System.exit(1);
				}
				cp = new ClosedPatterns(dataset, minFreq, p);
				c = new Constraint("Closed Patterns ++ Jaccard Evaluation", cp);
			}
			else if(argparam.length > 7) {
				method = argparam[7]; // CD and CDJ
				
				// diversity threshold
				jmax = Double.valueOf(argparam[4]);
				
				// search strategy
				searchStrategy = argparam[5];
				witnessStrategy = argparam[6];
				
				// number of thread
				try {
					nbThread = Integer.parseInt(argparam[8]);
				}catch (NumberFormatException e) {
					System.out.println("Error: the number of thread should be an integer.");
					e.printStackTrace();
					System.exit(1);
				}
				this.cd = new ClosedDiversity(dataset, minFreq, jmax, history.getAllItemsets(), p);
				c = new Constraint("Closed Diversity", cd);
			}
			
			model.post(c);
		} catch (IOException | ContradictionException e) {
			out.print(e);
		}
	}
	
	
	public void configureSearch() {
		Solver s = model.getSolver();
		
		if((method.equals("CP")) || (method.equals("CPJ")) || (method.equals("CPD"))) {
			s.setSearch(minDomUBSearch(p));
		}
		else {
			if(searchStrategy.equals("MINCOV")){
				ClosedDiversityStrategies.defineMincovSearchStrategy(model, cd,  p);
			}
			else if(searchStrategy.equals("WITNESS")) {
				ClosedDiversityStrategies.defineWitnessSearchStrategy(model, cd, p);
			}
			else {
				model.getSolver().setSearch(minDomUBSearch(p));
			}
		}
	}
	
	
	public void addToHistory(BitSet candidat){
		history.addSolutionCD(new Solution(candidat), cd.cov, 0);
		cd.setHistory(history.getAllItemsets());
		cd.setNbSolutions(history.getNbElement());
		nbSolutions = history.getNbElement();
	}
	
	
	public void solveClosedDiversity(Solver s) {
		out.println("SOLVE ClosedDiversity : --> minFreq = " + minFreq + "/" + dataset.getTransactionsSize() + ", Jmax = " + jmax);
		
		MemoryLogger.getInstance().reset();
		while(s.solve()) {
			BitSet candidat = new BitSet(); 
			for (int item = 0; item < dataset.getNbrVar(); item++) {
				if (p[item].isInstantiatedTo(1))
					candidat.set(item); // candidate' items
			}
			if(candidat.isEmpty())
				continue; // reject empty pattern
			
			if(!isWitness){
				if(method.equals("CDJ")) {
					if (nbThread > 0) {
						if (!(test_jaccard_THREADS(candidat)))
							continue;
					} else if(!(test_jaccard(candidat)))
							continue;
				}
				
				addToHistory(candidat);
				
				if(witnessStrategy.equals("WITNESSFIRSTSOL"))
					s.setJumpTo(1);
				else if(witnessStrategy.equals("WITNESSDIVSOL") && (witnessItem != -1)) {
					Solution[] witnessSol = new Solution[witnessNodeSolutions.length+1];
					for(int i = 0; i < witnessNodeSolutions.length; i++)
						witnessSol[i] = witnessNodeSolutions[i];
					witnessSol[witnessNodeSolutions.length] = new Solution((BitSet) candidat.clone());
					witnessNodeSolutions = witnessSol.clone();
					nbTotalSolutionsWitnessNode++;
					history.getAllItemsets()[nbSolutions-1].setLabel(nbWitnessNode);
				}
			}
			else{ // pattern is witness
				addToHistory(candidat);
				
				nbTotalWitnessSolutions++;
				history.getAllItemsets()[nbSolutions-1].setLabel(nbWitnessNode);
				
				if(witnessStrategy.equals("WITNESSFIRSTSOL")) {
					s.setJumpTo(this.cd.jto);
					nbTotalSolutionsWitnessNode++;
				}
				else if(witnessStrategy.equals("WITNESSDIVSOL")){
					witnessNodeSolutions = new Solution[1];
					witnessNodeSolutions[0] = new Solution((BitSet) candidat.clone());
					nbTotalSolutionsWitnessNode++;
				}
				isWitness = false;
			}
			this.cd.jto = 0;
		}
		
		// --- POST-TREATMENT ---
		
		double cpu_time = s.getTimeCount();
		long nb_nodes = s.getNodeCount();
		long allSolutionsFound = s.getSolutionCount();
		
		File datasetFile = new File(pathInput);
		File outputFile = new File(pathOutput);
		/*
		String output_data = "" + datasetFile.getName() + ";" + outputFile.getName() + ";" +
				((double) minFreq/dataset.getTransactionsSize()) + ";" + jmax + ";" + cpu_time +
				";" + allSolutionsFound + ";" + nb_nodes + ";" + nbTotalWitnessSolutions + ";" +
				this.cd.numberVarFiltredByLB + ";" + this.searchStrategy + "\n";
		//*/
		
		String output_data = "" + datasetFile.getName() + ";" + outputFile.getName() + ";" +
				((double) minFreq/dataset.getTransactionsSize()) + ";" + jmax + ";" + cpu_time +
				";" + allSolutionsFound + ";" + nbSolutions + ";" + nb_nodes + ";" + nbTotalWitnessSolutions +
				";" + this.cd.numberVarFiltredByLB + ";" + this.nbSolutionsFilteredByJaccard +
				";" + this.searchStrategy + "\n";

		File file = new File(pathAnalyse);
		FileWriter fr;
		try {
			// save summary of the results
			fr = new FileWriter(file, false);
			fr.write(output_data);
			fr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		file = new File(pathOutput);
		try {
			// save patterns and their covers
			fr = new FileWriter(file, false);
			this.history.getCDSolutionsWithCover(fr);
			fr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("\n************************************\n");
		MemoryLogger.getInstance().checkMemory();
		
		System.out.println("Jmax = " + ClosedDiversity.jMax);
		System.out.println("CPU Time = " + s.getTimeCount());
		System.out.println("Max Memory = " + MemoryLogger.getInstance().getMaxMemory());
		System.out.println("All solutions count = " + nbSolutions);
		System.out.println("All candidates found = " + allSolutionsFound);
		System.out.println("Nb of witness solutions = " + this.nbTotalWitnessSolutions);
		System.out.println("nb de solutions issues des noeuds witness = " + this.nbTotalSolutionsWitnessNode);
		System.out.println("Nb Nodes = " + s.getNodeCount());
		System.out.println("Nb witness nodes = " + this.nbWitnessNode);
		System.out.println("Nb items filtred by LB = " + this.cd.numberVarFiltredByLB);
		System.out.println("Candidates Filtered by Jaccard ~: " + (nbSolutionsFilteredByJaccard));
		System.out.println("Nb of Fails = " + s.getFailCount());
		System.out.println("Search Strategy = " + searchStrategy);
		System.out.println("Witness Strategy = " + witnessStrategy);
		System.out.println("***** END *****");
		
		
	}
	
	
	public void solveClosedPattern(Solver s) {
		out.println("SOLVE ClosedPattern : --> minFreq = " + minFreq + "/" + dataset.getTransactionsSize());
		
		MemoryLogger.getInstance().reset();
		while(s.solve()) {
			BitSet candidat = new BitSet(); 
			for (int item = 0; item < dataset.getNbrVar(); item++) {
				if (p[item].isInstantiatedTo(1))
					candidat.set(item); // candidate' items
			}
			if(candidat.isEmpty())
				continue; // reject empty pattern
			if((method.equals("CPJ")) && (!test_jaccard(candidat)))
				continue;
			
			history.addSolutionCP(new Solution(candidat));
		}
		double time = s.getTimeCount();
		long nb_nodes = s.getNodeCount();
		long nb_solutions = history.getNbElement();
		
		File datasetFile = new File(pathInput);
		
		// --- POST-TREATMENT ---
		MemoryLogger.getInstance().checkMemory();
		out.println("#\t \t \t *************************** LOGS ***********************\n");
		out.println("dataset =>>> " + datasetFile.getName() + "\n");
		out.println("MinSupport  =>>> " + minFreq + "\n");
		out.println("#\t \t \t *************************** STATS ***********************\n");
		out.println("\t \t \t ------------------- Total time ~: " + time + " s --------------------\n");
		out.println("\t \t \t ------------------- Solutions Count ~: " + nb_solutions + "  --------------------\n");
		out.println("\t \t \t ------------------- All Candidates Found ~: " + s.getSolutionCount() + "  --------------------\n");
		out.println("\t \t \t ------------------- Candidates Filtered by Jaccard ~: " + (nbSolutionsFilteredByJaccard) + "  --------------------\n");
		out.println("\t \t \t ------------------- Max memory:" + MemoryLogger.getInstance().getMaxMemory() + "\n");
		out.println("\t \t \t ------------------- Backtrak Count :" + s.getBackTrackCount() + "\n");
		out.println("\t \t \t ------------------- Nodes Count :" + nb_nodes + "\n");
		out.println("\t \t \t ------------------- Fails Count :" + s.getFailCount() + "\n");
		
		File file = new File(pathOutput);
		FileWriter fr = null;
		try {
			fr = new FileWriter(file, false);
			this.history.getCPSolutionsWithCover(fr);
			fr.close();
		} catch(IOException e2) {
			e2.printStackTrace();
			System.exit(1);
		}

		//String output_data = argparam[1] + ";" + time + ";" + nb_solutions + ";" + nb_nodes + "\n";
		String output_data = file.getName() + ";" + time + ";" + nb_solutions + ";" + nb_nodes + "\n";
		
		file = new File(pathAnalyse);
		fr = null;
		try {
			fr = new FileWriter(file, false);
			fr.write(output_data);
			fr.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(1);
		}
		out.println("\t \t \t \t *****END OF THE POST-PROCESS *****");
	}
	
	
	public void solveClosedPattern_with_diversity(Solver s) {
		System.out.println("SOLVE ClosedPatterns + Div Constraint : --> minFreq = " + minFreq + 
				"/" + dataset.getTransactionsSize() + ", Jmax = " + jmax);
		System.out.println("------------------------------------------------------------------\n\n");
		
		//s.showDecisions();
		//s.showContradiction();
		//s.showStatistics();
		
		MemoryLogger.getInstance().reset();
		while(s.solve()) {
			BitSet candidat = new BitSet(); 
			for (int item = 0; item < dataset.getNbrVar(); item++) {
				if (p[item].isInstantiatedTo(1))
					candidat.set(item); // candidate' items
			}
			//System.out.println("::::::::::::::::::::::::::::::::::\n");
			if(candidat.isEmpty())
				continue; // reject empty pattern
			
			BitSet transactions = new BitSet();
			for (int tr = 0; tr < dataset.getTransactionsSize(); tr++) {
				if (T[tr].isInstantiatedTo(1))
					transactions.set(tr); // candidate' transactions
			}
			
			history.addSolutionCP(new Solution(candidat), transactions);
			nbSolutions = history.getNbElement();
			diversity_constraint(T, candidat, jmax);
			
		}
		
		// --- POST-TREATMENT ---
		
		MemoryLogger.getInstance().checkMemory();
		double cpu_time = s.getTimeCount();
		long nb_nodes = s.getNodeCount();
		long allSolutionsFound = s.getSolutionCount();
		long nb_fails = s.getFailCount();
		
		File datasetFile = new File(pathInput);
		File outputFile = new File(pathOutput);
		
		String output_data = "" + datasetFile.getName() + ";" + outputFile.getName() + ";" +
				((double) minFreq/dataset.getTransactionsSize()) + ";" + jmax + ";" + 
				cpu_time + ";" + allSolutionsFound + ";" + nbSolutions + ";" + 
				nb_nodes + ";" + nb_fails + ";NONE\n";
		

		System.out.println("\n\n*************************** LOGS ***********************\n");
		//System.out.println("Global Constraint ClosedP ++ Div Constraint");
		
		System.out.println("dataset =>>> " + datasetFile.getName());
		System.out.println("MinSupport  =>>> " + minFreq);
		System.out.println("Jmax = " + jmax);
		System.out.println("CPU Time = " + s.getTimeCount());
		System.out.println("All solutions count = " + nbSolutions);
		//System.out.println("All candidates found = " + allSolutionsFound);
		System.out.println("Max Memory = " + MemoryLogger.getInstance().getMaxMemory());
		System.out.println("Backtrak Count :" + s.getBackTrackCount());
		System.out.println("Nb Nodes = " + s.getNodeCount());
		System.out.println("Nb of Fails = " + s.getFailCount());
		System.out.println("Search Strategy = " + searchStrategy);
		System.out.println("***** END *****");

		File file = new File(pathOutput);
		FileWriter fr;
		try {
			// save patterns and their covers
			fr = new FileWriter(file, false);
			//this.history.getCPSolutionsWithCover(fr);
			this.history.getCPSolutions(fr);
			fr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		file = new File(pathAnalyse);
		try {
			// save summary of the results
			fr = new FileWriter(file, false);
			fr.write(output_data);
			fr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/*
	public void addToHistory(BitSet candidat){
		BitSet cov = dataset.covers.getCoverPOP(new TItemSet(candidat)).getListTransactions();
		history.addSolutionCD(new Solution(candidat), cov, 0);
		nbSolutions = history.getNbElement();
	}
	//*/
	
	public void diversity_constraint(BoolVar[] Tr, BitSet pattern, double jmax) {
		BitSet cov = dataset.covers.getCoverPOP(new TItemSet(pattern)).getListTransactions();
		int[] cover = cov_to_int(cov, Tr.length);
		
		int[] cste_tab = new int[Tr.length];
		int cste = 0;
		for(int t=0; t<Tr.length; t++) {
			cste_tab[t] = (int) Math.round(
					(cover[t] - jmax*(1-cover[t]))*100); // H[q,t] - Jmax*(1-H[q,t])
			cste += (int) Math.round(cover[t] * jmax * 100); // sum_t(Jmax*H[q,t])
		}
		
		// On poste ici la contrainte de diversité
		model.scalar(Tr, cste_tab, "<=", cste).post();
	}
	
	private int[] cov_to_int(BitSet cov, int nb) {
		int[] int_cov = new int[nb];
		for(int i=0; i<nb; i++) {
			int_cov[i] = cov.get(i) ? 1 : 0;
		}
		return int_cov;
	}
	
	
	public void solve() {
		Solver s = model.getSolver();
		if(method.equals("CP") || method.equals("CPJ")) {
			solveClosedPattern(s);
		}
		else if(method.equals("CPD")) {
			solveClosedPattern_with_diversity(s);
		}
		else if(method.equals("CD") || method.equals("CDJ")) {
			solveClosedDiversity(s);
		}
	}
	
	public boolean test_jaccard(BitSet cand) {
		boolean test_jaccard = true;
		double jac = 0;
		BitSet cov_X = new BitSet();
		cov_X = dataset.coversBorne.getCoverPOP(new TItemSet(cand)).getListTransactions();
		for (int i=0; i < history.getNbElement(); i++) {
			BitSet cov_Hi = new BitSet();
			BitSet cov_intersect = new BitSet();
			cov_intersect = (BitSet) cov_X.clone();
			cov_Hi = dataset.coversBorne.getCoverPOP(new TItemSet(history.getAllItemsets()[i].
					getItemset())).getListTransactions();
			cov_intersect.and(cov_Hi);
			double num = (double) cov_intersect.cardinality();
			double denom = (double) cov_X.cardinality() + cov_Hi.cardinality() - cov_intersect.cardinality();
			jac = (double) num/denom;
			
			if (jac > ClosedDiversity.jMax) {
				test_jaccard = false;
				this.nbSolutionsFilteredByJaccard += 1;
				break;
			}
		}
		return test_jaccard;
		
	}
	
	
	public synchronized void setJaccard(boolean bl) {
		continueJaccard = bl;
	}
	
	
	public boolean test_jaccard_THREADS(BitSet cov_X) {
		Thread[] tabThread = new Thread[nbThread];
		int[] partition = new int[nbThread+1];
		for(int i = 0; i < partition.length; i++)
			partition[i] = (nbSolutions * i) / nbThread;
		
		for (int ind=0 ; ind < tabThread.length; ind++) {
			int j = ind;
			tabThread[ind] = new Thread("Thread " + (ind + 1)) {
				public void run() {
					for(int i = partition[j]; i < partition[j+1]; i++) {
						BitSet cov_Hi = new BitSet();
						BitSet cov_intersect = new BitSet();
						cov_intersect = (BitSet) cov_X.clone();
						cov_Hi = history.getAllItemsets()[i].getCovX();
						
						cov_intersect.and(cov_Hi);
						double num = (double) cov_intersect.cardinality();
						double denom = (double) cov_X.cardinality() + cov_Hi.cardinality() - cov_intersect.cardinality();
						double jac = num/denom;
						
						if (continueJaccard && (jac > ClosedDiversity.jMax)) {
							nbSolutionsFilteredByJaccard  += 1;
							setJaccard(false);
							break;
						}
						
						if (!continueJaccard)
							break;
					}
				}
			};
		}
		for (int ind = 0; ind < tabThread.length; ind++)
			tabThread[ind].start();
		// wait for the end of all threads
		try {
			for (int ind = 0; ind < tabThread.length; ind++)
				tabThread[ind].join();
		} catch (Exception e) {
			System.out.println("Interrupted");
		}
		
		return continueJaccard;
		
	}
	
	
	public static String[] parseParam(String[] args) {
		String msg = "The minimum command syntax is :\n";
		msg += "JAVA -JAR PROGRAMM CONSTRAINT datasetPath ";
		msg += "resultsPatternsFilePath resultsSummaryFilePath ";
		msg += "-f absoluteFrequency OPTIONS \n";
		msg += "CONSTRAINT = \n";
		msg += "\t" + "CP : ClosedPattern\n";
		msg += "\t" + "CD : ClosedDiversity\n";
		msg += "\t" + "CPJ : ClosedPattern + Jaccard Consistancy evaluation\n";
		msg += "\t" + "CPD : ClosedPattern + Diversity Constraint\n";
		msg += "\t" + "CDJ : ClosedDiversity + Jaccard Consistancy evaluation\n\n";
		msg += "datasetPath : dataset file path\n";
		msg += "resultsPatternsFilePath : result itemsets file path\n";
		msg += "resultsSummaryFilePath : file for some logs data\n\n";
		msg += "OPTIONS = \n";
		msg += "\t" + "-f [num]: the frequency absolute value (between 0 and total number of transactions)\n";
		msg += "\t" + "-j [threshold]: the diversity threshold (float between 0 and 1)\n";
		msg += "\t" + "-s [strategy] [witness strategy]: the search strategy [MINCOV NONE, WITNESS WITNESSFIRSTSOL, WITNESS WITNESSDIVSOL]\n";
		msg += "\t" + "-th [num]: the number of threads to use for jaccard evaluation \n\n";
		
		msg += "The second, third and fourth arguments must be set for the dataset, ";
		msg += "the itemsets file and the logs summary file \n";
		
		if(args.length < 5) {
			System.out.println(msg);
			System.exit(1);
		}
		
		String datasetPath = args[1];
		String resultPath = args[2];
		String summaryPath = args[3];
		File f1 = new File(datasetPath), f2 = new File(resultPath), f3 = new File(summaryPath);
		File d1 = new File(f1.getParent()), d2 = new File(f2.getParent()), d3 = new File(f3.getParent());
		if(!f1.exists() || !f2.exists() || !f3.exists() || !d1.isDirectory() || !d2.isDirectory() || !d3.isDirectory()) { 
			System.out.println("The second, third and fourth arguments must be set for the dataset, the itemsets file and the logs summary file \n");
			System.exit(1);
		}
		
		String constraint = args[0];
		if(!constraint.equals("CP") && !constraint.equals("CD") && !constraint.equals("CPD") 
				&& !constraint.equals("CPJ") && !constraint.equals("CDJ")) {
			System.out.println("The \'CONSTRAINT\' parameter must be in the list [CP, CPD, CD, CPJ, CDJ]\n");
			System.out.println(msg);
			System.exit(1);
		}
		
		String params = "";
		for(int i=4; i<args.length; i++) 
			params = params + args[i] + " ";
		
		// -------------------------------------------------------------------------------------
		// -------------------------------------------------------------------------------------
		
		// get frequency
		Pattern p = Pattern.compile("(\\-)[f](\\s)(\\d)+");
		Matcher m = p.matcher(params);
		int minsup = -1;
		while (m.find()) { 
			String s = m.group();
			if(s.split(" ").length < 2) {
				System.out.println("specify the frequency threshold in the form \'-f "
        					+ "VALUE\' (VALUE is the absolute threshold (an Integer))");
				System.exit(1);
			}
			minsup = Integer.parseInt(s.split(" ")[1]);
			break;
		}
		if(minsup < 0) {
			System.out.println("the frequency threshold must be greater than 0");
			System.exit(1);
		}
        	
		// -------------------------------------------------------------------------------------
		// -------------------------------------------------------------------------------------
		
		// get jmax
		double jmax = 0.0;
		if(constraint.equals("CD") || constraint.equals("CPD") || constraint.equals("CPJ") 
				|| constraint.equals("CDJ")) {
			p = Pattern.compile("(\\-)[j](\\s)[0](\\.)(\\d)+");
			m = p.matcher(params);
			while (m.find()) { 
				String s = m.group();
				if(s.split(" ").length < 2) {
					System.out.println("specify the diversity threshold jmax in the form \'-j "
							+ "VALUE\' (VALUE is an Float)");
					System.exit(1);
				}
				jmax = Double.parseDouble(s.split(" ")[1]);
				break;
			}
			if((jmax > 1) || (jmax <= 0)) {
				System.out.println("the diversity threshold jmax must be between 0 and 1");
				System.out.println(msg);
				System.exit(1);
			}
		}
		
		// -------------------------------------------------------------------------------------
		// -------------------------------------------------------------------------------------
		
		// get search strategy
		String searchStrategy = "MINCOV", witnessStrategy = "NONE"; // default
		if(constraint.equals("CD") || constraint.equals("CDJ")) {
			p = Pattern.compile("(\\-)[s](\\s)[a-zA-Z]+.*");
			m = p.matcher(params);
			while (m.find()) {
				String s = m.group();
				if(s.split(" ").length < 2) {
					System.out.println("specify the search strategy in the form \'-s "
							+ "VALUE [OPTION]\'");
					System.out.println(msg);
					System.exit(1);
				}
				String strategy = s.split(" ")[1].toUpperCase();
				if(strategy.equals("WITNESS")) {
					searchStrategy = strategy;
					witnessStrategy = "WITNESSFIRSTSOL";
					if((s.split(" ").length > 2) && (!s.split(" ")[2].startsWith("-"))){
						String wStrat = s.split(" ")[2].toUpperCase();
						if((wStrat.equals("WITNESSFIRSTSOL")) && (wStrat.equals("WITNESSDIVSOL"))) {
							System.out.println("As you have set the search strategy to \'WITNESS\' "
								+ "you must set the witness strategy either to "
								+ "\'WITNESSFIRSTSOL\' or \'WITNESSDIVSOL\'");
							System.out.println(msg);
							System.exit(1);
						}
						else {
							witnessStrategy = wStrat;
						}
					}
				}
	        		else if(strategy.equals("MINCOV")) {
					searchStrategy = strategy;
					witnessStrategy = "NONE";
				}
				else {
					System.out.println("The search strategy value must be either "
						+ "\'MINCOV\' or \'WITNESS\'");
					System.out.println(msg);
					System.exit(1);
				}
			}
			
		}
        	
		// -------------------------------------------------------------------------------------
		// -------------------------------------------------------------------------------------
		
		int nbThreads = 0; // default
		if(constraint.equals("CPJ") || constraint.equals("CDJ")) {
			// get the number of threads
			p = Pattern.compile("(\\-)[th](\\s)(\\d)+");
			m = p.matcher(params);
			while (m.find()) { 
				String s = m.group();
				if(s.split(" ").length < 2) {
					System.out.println("specify the number of threads in the form \'-th "
							+ "VALUE\' (VALUE is an Integer)");
					System.out.println(msg);
					System.exit(1);
				}
				nbThreads = Integer.parseInt(s.split(" ")[1]);
				break;
			}
			if(nbThreads < 0) {
				System.out.println("the number of threads can not be negative");
				System.out.println(msg);
				System.exit(1);
			}
		}
		
		String res = "";
		if(constraint.equals("CP")) {
			res = datasetPath + " " + resultPath + " " + summaryPath + " " + minsup + " "
					+ constraint + " " + nbThreads + "";
		}
		else if(constraint.equals("CPJ")) {
			res = datasetPath + " " + resultPath + " " + summaryPath + " " + minsup + " " 
					+ jmax + " " + constraint + " " + nbThreads + "";
		}
		else if(constraint.equals("CPD")) {
			res = datasetPath + " " + resultPath + " " + summaryPath + " " + minsup + " " 
					+ jmax + " " + constraint + "";
		}
		else if(constraint.equals("CD") || constraint.equals("CDJ")) {
			res = datasetPath + " " + resultPath + " " + summaryPath + " " + minsup + " " 
					+ jmax + " " + searchStrategy + " " + witnessStrategy + " " 
					+ constraint + " " + nbThreads + "";
		}
		
		return res.split(" ");
	}
	
	
	public static void main(String[] args) throws IOException {
		argparam = parseParam(args);
		new RunMain().execute();

	}

}
