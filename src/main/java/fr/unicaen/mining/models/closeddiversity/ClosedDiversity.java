package fr.unicaen.mining.models.closeddiversity;

//import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
//import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

import fr.unicaen.mining.datasets.DataSet;
import fr.unicaen.mining.RunMain;
import fr.unicaen.mining.structures.Solution;
import fr.unicaen.mining.structures.TItemSet;
import fr.unicaen.mining.structures.TTransactionSet;

public class ClosedDiversity extends Propagator<BoolVar> {
	public int seuil;
	public int nbSolutions;
	public static double jMax;
	
	public BoolVar[] p;
	public DataSet dataset;
	//public BitSet[] history_itemsets;
	public Solution[] history_itemsets;
	
	/*
	private int[] estimatedFrequenciesActual;
	private Deque<Integer> allCoverSigmaPlus;
	private Deque<int[]> allEstimatedFrequencies;
	//*/
	
	private IStateInt btrk_coverSigmaPlus;
	private IStateInt[] btrk_estimatedFrequencies;
	
	public int nextVar;
	public int jto = 0;
	public int numberVarFiltredByLB = 0;
	
	public BitSet cov = new BitSet();
	public BitSet itemset = new BitSet();
	
	//static int count = 0;

	//public ClosedDiversity(DataSet dataset, int seuil, double ja, BitSet[] history, BoolVar[] p) {
	public ClosedDiversity(DataSet dataset, int seuil, double ja, Solution[] history, BoolVar[] p) {
		// initializations for ClosedPatterns
		super(p);
		this.p = p;
		this.seuil = seuil;
		this.dataset = dataset;

		// initializations for ClosedDiversity
		jMax = (double) ja;
		this.nbSolutions = 0;
		this.history_itemsets = history;
		
		// backtrackable structures : estimated frequencies and covers of X+
		btrk_estimatedFrequencies = new IStateInt[dataset.getNbrVar()];
		for (int item = 0; item < dataset.getNbrVar(); item++)
			btrk_estimatedFrequencies[item] = this.model.getEnvironment().makeInt(
					dataset.getVerticalDataBase()[item].cardinality());
		btrk_coverSigmaPlus = this.model.getEnvironment().makeInt(dataset.getTransactionsSize());
		
		nextVar = -1;
	}
	
	public void check_consistancy(BitSet itemset, BitSet cover) throws ContradictionException {
		if(!itemset.isEmpty() && !patternGrowth_LB(cover)) {
			jto = 0;
			nextVar = -1;
			RunMain.currentItem = -1;
			model.getSolver().setJumpTo(1);
			fails();
		}
	}
	
	@Override
	public void propagate(int evtmask) throws ContradictionException {
		boolean change = false, fullExt = false;
		
		// les structures internes
		BitSet free_items = new BitSet();
		BitSet filtered_items = new BitSet();
		BitSet current_itemset_items = new BitSet();
		for (int item = 0; item < dataset.getNbrVar(); item++) {
			if (p[item].isInstantiatedTo(0)) {
				filtered_items.set(item);
			} else if (p[item].isInstantiatedTo(1)) {
				current_itemset_items.set(item);
			} else
				free_items.set(item);
		}
		
		//System.out.println("PROPAGATE-CLOSEDDIV\nsigma+ = " + current_itemset_items);
		
		//count++;
		
		/*
		System.out.println("propagate_closedDiv"); //: count=" + count);
		System.out.println("sigma+ = " + current_itemset_items);
		System.out.println("sigma- = " + filtered_items);
		System.out.println("sigma* = " + free_items + "\n");
		//*/
		
		// cov(X)
		TTransactionSet coverPos1 = dataset.covers.getCoverPOP(new TItemSet(current_itemset_items));
		check_consistancy(current_itemset_items, coverPos1.getListTransactions());
		
		int diff = 0;
		diff = btrk_coverSigmaPlus.get() - coverPos1.getListTransactions().cardinality();
		btrk_coverSigmaPlus.set(coverPos1.getListTransactions().cardinality());
		
		int min_freq=coverPos1.getListTransactions().cardinality()+1;
		BitSet free_items_prime = (BitSet) free_items.clone();
		
		for (int item=free_items.nextSetBit(0); item!=-1; item=free_items.nextSetBit(item+1)) {
			
			// Estimated frequencies
			int a = btrk_estimatedFrequencies[item].get() - diff;
			if(a < seuil) {
				BitSet cov_XUx = dataset.covers.intersectCover(coverPos1, item).
						getListTransactions();
				
				a = cov_XUx.cardinality();
				if (a < seuil) { // frequency filtering
					btrk_estimatedFrequencies[item].set(a);
					p[item].removeValue(1, Cause.Null);
					filtered_items.set(item);
					change = true;
					//System.out.println("filtrage - frequence : item " + item);
					continue;
				}
			}
			btrk_estimatedFrequencies[item].set(a);
			
			// full-extension
			if (dataset.covers.checkSameCover(coverPos1, item)) {
				current_itemset_items.set(item);
				p[item].removeValue(0, Cause.Null);
				dataset.covers.pushCover(new TItemSet(current_itemset_items), coverPos1);
				fullExt = true;
				//System.out.println("full-ext : item " + item);
				continue;
			}
			
			// filtering by LB
			if (!RunMain.isWitness && !patternGrowth_LB(dataset.covers.intersectCover(coverPos1, item).getListTransactions())) { 
				p[item].removeValue(1, Cause.Null);
				filtered_items.set(item);
				change = true;
				numberVarFiltredByLB++;
				//System.out.println("filtrage LB : item " + item);
				continue;
			}
			// Éléments absents
			for (int k = filtered_items.nextSetBit(0); k != -1; k = filtered_items.nextSetBit(k + 1)) {
				TTransactionSet cover01 = dataset.covers.intersectCover(coverPos1, k); 
				if (coverInclusion(cover01, dataset.covers.intersectCover(coverPos1, item))) {
					p[item].removeValue(1, Cause.Null);
					filtered_items.set(item);
					change = true;
					//System.out.println("absents : item " + item);
					break;
				}
			}
			
			// Update the next variable to be instanciated to 1 : 
			// it will be (by default) the next free variable with the lowest estimated Frequency
			if(!change && a < min_freq) {
				min_freq = a;
				nextVar = item;
			}
		}
		
		int item = free_items.nextSetBit(0);
		if ((free_items.cardinality() == 1) && (free_items.equals(free_items_prime)) && 
				(!p[item].isInstantiated())) {
			int val = -1;
			
			BitSet temp = (BitSet) current_itemset_items.clone();
			ArrayList<BitSet> h = new ArrayList<BitSet>();
			BitSet cop = new BitSet();
			temp.set(item);
			
			if(RunMain.isWitness) {
				h.add(current_itemset_items);
				cop = dataset.covers.intersectCover(coverPos1, item).getListTransactions();
				val = 1;
			}
			else {
				h.add(temp);
				cop = coverPos1.getListTransactions();
				val = 0;
			}
			if (calcul_LB(cop, h, h.size()) > jMax) {
				p[item].removeValue(val, Cause.Null);
				nextVar = -1;
				change = true;
			}
		}
		
		if(!change && !fullExt && RunMain.isWitness)
			jto++;
		
		cov = (BitSet) coverPos1.getListTransactions().clone();
		itemset = (BitSet) current_itemset_items.clone();
		
		BitSet s_positif = new BitSet(), s_negatif = new BitSet(), s_libre = new BitSet();
		for (int i = 0; i < dataset.getNbrVar(); i++) {
			if (p[i].isInstantiatedTo(1))
				s_positif.set(i);
			else if (p[i].isInstantiatedTo(0))
				s_negatif.set(i);
			else
				s_libre.set(i);
		}
		/*
		System.out.println("END PROPAGATE-CLOSEDDIV");
		System.out.println("sigma+ = " + s_positif);
		System.out.println("sigma- = " + s_negatif);
		System.out.println("sigma* = " + s_libre + "\n");
		//*/
	}


	@Override
	public ESat isEntailed() {
		return ESat.TRUE;
	}

	public boolean condition(TTransactionSet coverPos1, List<Integer> Sigma_ne) {
		for (Integer item : Sigma_ne) {
			if (dataset.covers.checkSameCover(coverPos1, item)) {
				return true;
			}
		}
		return false;
	}

	public boolean coverInclusion(TTransactionSet cover1, TTransactionSet cover2) {
		BitSet T2 = new BitSet();
		BitSet T11 = new BitSet();
		T2 = cover2.getListTransactions();
		T11 = (BitSet) T2.clone();
		T11.andNot(cover1.getListTransactions());

		if (T11.isEmpty()) {
			return true;
		}
		return false;
	}

	// lobnury
	public static boolean inclusion(BitSet pattern1, BitSet pattern2) {
		// pattern1 included in pattern2
		BitSet p = new BitSet();
		p = (BitSet) pattern1.clone();
		p.andNot(pattern2);

		if (p.isEmpty()) {
			return true;
		}
		return false;
	}
	
	// lobnury
	public boolean patternGrowth_LB(BitSet cov_XUx) {
		boolean grow = true;
		grow = growth_LB(cov_XUx);
		return grow;
	}
	
	// lobnury
	public boolean growth_LB(BitSet cov_XUx) {
		boolean growth_lb = true;

		for(int i = 0; i < this.nbSolutions; i++) {
			// list of transactions covered by history itemSet Hi
			BitSet cov_Hi = new BitSet();
			/*
			cov_Hi = dataset.coversBorne.getCoverPOP(new TItemSet(
						history_itemsets[i])).getListTransactions();
			//*/
			cov_Hi = dataset.coversBorne.getCoverPOP(new TItemSet(
					history_itemsets[i].getItemset())).getListTransactions();
			BitSet covP_XUx = (BitSet) cov_XUx.clone();
			covP_XUx.andNot(cov_Hi); // couverture propre de X
			BitSet covP_Hi = (BitSet) cov_Hi.clone();
			covP_Hi.andNot(cov_XUx); // couverture propre de Hi
			
			double val_lb = 0.0;
			double numerateur = 0.0, denominateur = 0.0;
			
			if(covP_XUx.cardinality() < seuil) {
				numerateur = (double) seuil - covP_XUx.cardinality();
				denominateur = (double) cov_Hi.cardinality() + covP_XUx.cardinality();
				val_lb = numerateur / denominateur;
			}
			else if(covP_Hi.cardinality() < seuil) {
				numerateur = (double) seuil - covP_Hi.cardinality();
				denominateur = (double) cov_Hi.cardinality() + covP_XUx.cardinality();
				val_lb = numerateur / denominateur;
			}
			//*/
			
			if((val_lb > jMax) || ((val_lb == jMax) && (val_lb == 0))) {
				growth_lb = false;
				break;
			}
		}

		return growth_lb;
	}


	public double calcul_LB(BitSet cov_X, ArrayList<BitSet> history, int nbElt_hist) {
		double val_lb = 0.0;
		if (nbElt_hist != 0) { // |H|
			Iterator<BitSet> iter = history.iterator(); // Historique H

			while (iter.hasNext()) {
				BitSet temp = (BitSet) iter.next().clone(); // Hi € Historique

				// list of transactions covered by history itemSet Hi
				BitSet cov_Hi = new BitSet();
				cov_Hi = dataset.coversBorne.getCoverPOP(new TItemSet(temp)).getListTransactions();
				BitSet covP_x = (BitSet) cov_X.clone();
				covP_x.andNot(cov_Hi); // couverture propre de X
				BitSet covP_Hi = (BitSet) cov_Hi.clone();
				covP_Hi.andNot(cov_X); // couverture propre de Hi
				
				double numerateur = 0.0, denominateur = 0.0;
				
				if(covP_x.cardinality() < seuil) {
					numerateur = (double) seuil - covP_x.cardinality();
					denominateur = (double) cov_Hi.cardinality() + covP_x.cardinality();
					val_lb = numerateur / denominateur;
				}
				else if(covP_Hi.cardinality() < seuil) {
					numerateur = (double) seuil - covP_Hi.cardinality();
					denominateur = (double) cov_Hi.cardinality() + covP_x.cardinality();
					val_lb = numerateur / denominateur;
				}
				if ((val_lb > jMax)) //
					break;
			}
		}
		return val_lb;
	}
	
	// lobnury
	//public void setHistory(BitSet[] history) {
	public void setHistory(Solution[] history) {
		this.history_itemsets = history;
	}
	// lobnury
	public void setNbSolutions(int nbElt) {
		this.nbSolutions = nbElt;
	}
	// lobnury
	public int getNbElt(int nbElt) {
		return this.nbSolutions;
	}
}