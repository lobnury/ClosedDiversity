package constraints.closeddiv;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

import constraints.RunMain;
import constraints.dataset.DataSet;
import constraints.itemsets.Solution;
import constraints.tools.TItemSet;
import constraints.tools.TTransactionSet;

public class ClosedDiversity extends Propagator<BoolVar> {
	private int minFreq;
	private int nbSolutions;
	
	private BoolVar[] p;
	private DataSet dataset;
	private Solution[] history;
	
	private int[] estimatedFrequenciesActual;
	private Deque<Integer> allCoverSigmaPlus;
	private Deque<int[]> allEstimatedFrequencies;
	
	public static double jMax;
	
	public int jto = 0;
	public int nextVar = -1;
	public int numberVarFiltredByLB = 0;
	
	public BitSet cov = new BitSet();
	public BitSet sigma_plus = new BitSet();
	
	public ClosedDiversity(DataSet dataset0, int minFreq0, double jacc, Solution[] history, BoolVar[] p) {
		// initializations for ClosedPatterns
		super(p);
		this.p = p;
		this.dataset = dataset0;
		this.minFreq = minFreq0;

		// initializations for ClosedDiversity
		jMax = (double) jacc;
		this.nbSolutions = 0;
		this.history = history;
		
		// Estimated frequencies
		estimatedFrequenciesActual = getEstimatedFrequenciesFull(dataset0);
		allEstimatedFrequencies = new ArrayDeque<int[]>();
		int[] f = new int[estimatedFrequenciesActual.length];
		for(int i = 0; i < estimatedFrequenciesActual.length; i++)
			f[i] = estimatedFrequenciesActual[i];
		allEstimatedFrequencies.push(f);
		
		// All covers of X+
		allCoverSigmaPlus = new ArrayDeque<Integer>();
		allCoverSigmaPlus.push(dataset0.getTransactionsSize());
	}
	
	// used to initialize estimated frequencies
	static int[] getEstimatedFrequenciesFull(DataSet dataset) {
		int[] freq = new int[dataset.getNbrVar()];
		for (int item = 0; item < dataset.getNbrVar(); item++)
			freq[item] = dataset.getVerticalDataBase()[item].cardinality();
		return freq;
	}
	
	@Override
	public void propagate(int evtmask) throws ContradictionException {
		boolean change = false, fullExt = false;
		
		// internal structures
		BitSet free_items = new BitSet();
		BitSet filtered_items = new BitSet();
		BitSet current_itemset_items = new BitSet();
		for (int item = 0; item < dataset.getNbrVar(); item++) {
			if (p[item].isInstantiatedTo(0))
				filtered_items.set(item);
			else if (p[item].isInstantiatedTo(1))
				current_itemset_items.set(item);
			else
				free_items.set(item);
		}
		
		// cov(X)
		TTransactionSet coverPos1 = dataset.covers.getCoverPOP(new TItemSet(current_itemset_items));
		
		int diff = 0;
		diff = allCoverSigmaPlus.peek() - coverPos1.getListTransactions().cardinality();
		int min_freq=coverPos1.getListTransactions().cardinality()+1;
		
		BitSet free_items_prime = (BitSet) free_items.clone();
		
		//*
		if(!current_itemset_items.isEmpty() && !patternGrowth_LB(coverPos1.getListTransactions())) {
			nextVar = -1;
			RunMain.currentItem = -1;
			
			allCoverSigmaPlus.pop();
			allEstimatedFrequencies.pop();
			
			if(allEstimatedFrequencies.size() != 0) {
				for(int i = 0; i < estimatedFrequenciesActual.length; i++)
					estimatedFrequenciesActual[i] = allEstimatedFrequencies.peek()[i];
			}
			model.getSolver().setJumpTo(1);
			jto = 0;
			fails();
		}
		
		for (int item = free_items.nextSetBit(0); item != -1; item = free_items.nextSetBit(item + 1)) {
			
			// Estimated frequencies
			estimatedFrequenciesActual[item] = allEstimatedFrequencies.peek()[item] - diff;
			if(estimatedFrequenciesActual[item] < minFreq) {
				BitSet cov_XUx = dataset.covers.intersectCover(coverPos1, item).
						getListTransactions();
				
				estimatedFrequenciesActual[item] = cov_XUx.cardinality();
				if (estimatedFrequenciesActual[item] < minFreq) { // frequency filtering
					p[item].removeValue(1, Cause.Null);
					filtered_items.set(item);
					change = true;
					continue;
				}
			}
			
			if (dataset.covers.checkSameCover(coverPos1, item)) { // full-extension
				current_itemset_items.set(item);
				p[item].removeValue(0, Cause.Null);
				dataset.covers.pushCover(new TItemSet(current_itemset_items), coverPos1);
				fullExt = true;
				continue;
			}
			
			// filtering by LB
			if (!RunMain.isWitness && !patternGrowth_LB(dataset.covers.intersectCover(coverPos1, item).getListTransactions())) { 
				p[item].removeValue(1, Cause.Null);
				filtered_items.set(item);
				change = true;
				numberVarFiltredByLB++;
				continue;
			}
			
			for (int k = filtered_items.nextSetBit(0); k != -1; k = filtered_items.nextSetBit(k + 1)) {
				TTransactionSet cover01 = dataset.covers.intersectCover(coverPos1, k); 
				if (coverInclusion(cover01, dataset.covers.intersectCover(coverPos1, item))) {
					p[item].removeValue(1, Cause.Null);
					filtered_items.set(item);
					change = true;
					break;
				}
			}
			
			// Update the next variable to be instanciated to 1 : 
			// it will be (by default) the next free variable with the lowest estimated Frequency
			if(!change && estimatedFrequenciesActual[item] < min_freq) {
				min_freq = estimatedFrequenciesActual[item];
				nextVar = item;
			}
		}
		
		int item = free_items.nextSetBit(0);
		if ((free_items.cardinality() == 1) && (free_items.equals(free_items_prime)) && 
				(!p[item].isInstantiated())) {
			
			BitSet temp = (BitSet) current_itemset_items.clone();
			temp.set(item);
			ArrayList<BitSet> h = new ArrayList<BitSet>();
			
			double lb_bound = 0.0;
			int val = -1;
			
			if(RunMain.isWitness) {
				h.add(current_itemset_items);
				BitSet co = dataset.covers.intersectCover(coverPos1, item).getListTransactions();
				lb_bound = eval_LB(co, h, h.size());
				val = 1;
			}
			else {
				h.add(temp);
				lb_bound = eval_LB(coverPos1.getListTransactions(), h, h.size());
				val = 0;
			}
			
			if (lb_bound > jMax) {
				p[item].removeValue(val, Cause.Null);
				nextVar = -1;
				change = true;
			}
		}
		
		if(!change && !fullExt && RunMain.isWitness) {
			jto++;
		}
		
		if(free_items.isEmpty()) {
			if(RunMain.witnessStrategy.equals("WITNESSFIRSTSOL")) {

				int fin = Math.max(1, jto);
				for(int iter = 0; iter < fin; iter++) {
					allCoverSigmaPlus.pop();
					if(allEstimatedFrequencies.size() != 0) {
						for(int i = 0; i < estimatedFrequenciesActual.length; i++)
							estimatedFrequenciesActual[i] = allEstimatedFrequencies.peek()[i];
					}
					allEstimatedFrequencies.pop();
				}
			}
			else {
				allCoverSigmaPlus.pop();
				if(allEstimatedFrequencies.size() != 0)
					for(int i = 0; i < estimatedFrequenciesActual.length; i++)
						estimatedFrequenciesActual[i] = allEstimatedFrequencies.peek()[i];
				allEstimatedFrequencies.pop();
			}
		}
		else if(!change && !fullExt) {
			int[] f = new int[estimatedFrequenciesActual.length];
			for(int i = 0; i < estimatedFrequenciesActual.length; i++)
				f[i] = estimatedFrequenciesActual[i];
			allEstimatedFrequencies.push(f);
			allCoverSigmaPlus.push(coverPos1.getListTransactions().cardinality());
		}

		sigma_plus = (BitSet) current_itemset_items.clone();
		cov = (BitSet) coverPos1.getListTransactions().clone();
		
	}
	
	@Override
	public ESat isEntailed() {
		return ESat.TRUE;
	}
	
	public boolean condition(TTransactionSet coverPos1, List<Integer> Sigma_ne) {
		for (Integer item : Sigma_ne) {
			if (dataset.covers.checkSameCover(coverPos1, item))
				return true;
		}
		return false;
	}
	
	public boolean coverInclusion(TTransactionSet cover1, TTransactionSet cover2) {
		BitSet T2 = new BitSet();
		BitSet T11 = new BitSet();
		T2 = cover2.getListTransactions();
		T11 = (BitSet) T2.clone();
		T11.andNot(cover1.getListTransactions());
		if (T11.isEmpty())
			return true;
		return false;
	}
	
	public static boolean inclusion(BitSet pattern1, BitSet pattern2) {
		// pattern1 included in pattern2
		BitSet p = new BitSet();
		p = (BitSet) pattern1.clone();
		p.andNot(pattern2);
		if (p.isEmpty())
			return true;
		return false;
	}
	
	public boolean patternGrowth_LB(BitSet cov_X) {
		boolean growth_lb = true;
		for(int i = 0; i < this.nbSolutions; i++) {
			// list of transactions covered by history itemSet Hi
			BitSet cov_Hi = new BitSet();
			cov_Hi = dataset.coversBorne.getCoverPOP(new TItemSet(history[i].getItemset())).getListTransactions();
			// Proper cover of XUx
			BitSet covP_XUx = (BitSet) cov_X.clone();
			covP_XUx.andNot(cov_Hi);
			// LB(XUx, H)
			double numerateur = (double) minFreq - covP_XUx.cardinality();
			double denominateur = (double) cov_Hi.cardinality() + covP_XUx.cardinality();
			double val_lb = numerateur / denominateur;
			val_lb = Math.max(val_lb, 0);

			if((val_lb > jMax) || ((val_lb == jMax) && (val_lb == 0))) {
				growth_lb = false;
				break;
			}
		}
		return growth_lb;
	}
	
	public double eval_LB(BitSet cov_X, ArrayList<BitSet> history, int nbElt_hist) {
		double val_lb = 0;
		if (nbElt_hist != 0) { // |H|
			Iterator<BitSet> iter = history.iterator(); // History H
			while (iter.hasNext()) {
				// Hi € History
				BitSet temp = (BitSet) iter.next().clone();
				// list of transactions covered by history itemSet Hi
				BitSet cov_Hi = new BitSet();
				cov_Hi = dataset.coversBorne.getCoverPOP(new TItemSet(temp)).getListTransactions();
				// proper cover of X
				BitSet covP_x = (BitSet) cov_X.clone();
				covP_x.andNot(cov_Hi);
				// LB
				double num = (double) minFreq - covP_x.cardinality();
				double denom = (double) cov_Hi.cardinality() + covP_x.cardinality();
				val_lb = num / denom;
				if ((val_lb > jMax)) //
					break;
			}
		}
		return val_lb;
	}
	
	public void setHistory(Solution[] history) {
		this.history = history;
	}
	public void setNbSolutions(int nbElt) {
		this.nbSolutions = nbElt;
	}
	public int getNbElt(int nbElt) {
		return this.nbSolutions;
	}

}
