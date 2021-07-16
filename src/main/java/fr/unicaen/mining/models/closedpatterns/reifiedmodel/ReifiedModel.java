package fr.unicaen.mining.models.closedpatterns.reifiedmodel;

import java.util.BitSet;
//import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import fr.unicaen.mining.datasets.DataSet;
import fr.unicaen.mining.structures.IncrementCovers;
import fr.unicaen.mining.structures.TItemSet;

public class ReifiedModel {
	private final int minFreq;
	private final int nbItems;
	private final int nbTransactions;
	
	private BitSet[] dataset;
	IncrementCovers covers;
	
	private Model model;
	private BoolVar[] P;
	private BoolVar[] T;
	
	public Map<BitSet, BitSet> history_covers = new HashMap<BitSet, BitSet>();
	
	public ReifiedModel(Model model, BoolVar[] p,  BoolVar[] t, DataSet dataset, int support) {
		this.minFreq = support;
		
		this.nbItems = dataset.getNbrVar();
		this.nbTransactions = dataset.getTransactionsSize();
		
		this.model = model;
		this.dataset = dataset.getVerticalDataBase();
		this.covers = new IncrementCovers(dataset);
		
		/*
		for(int i = 0; i< this.dataset.length; i++)
			System.out.println(this.dataset[i]);
		//*/
		
		this.P = p;
		this.T = t;
	}
	
	public void cover_constraint() {
		int[][] rows = rows();
		for(int t=0; t<nbTransactions; t++) {
			T[t] = model.scalar(P, rows[t], "=", 0).reify(); // Tt=1) iff SUM_i(Pi(1-D_ti) = 0) for all t
		}
	}
	
	public void minFreq_constraint() { 
		//*
		String fID = "minFreq";
		IntVar freq = model.intVar(fID, minFreq, nbTransactions);
		model.count(1, T, freq).post();
		//*/
		
		//*
		// contraintes redondantes
		for(int i=0; i<nbItems; i++) {
			int[] c = col_1(i);
			model.ifThen(P[i], model.scalar(T, c, ">=", minFreq));
		}
		//*/
	}
	
	public void closed_constraint() {
		for(int i=0; i<nbItems; i++) {
			int[] c = col_2(i);
			P[i] = model.scalar(T, c, "=", 0).reify();
		}
	}
	
	public void basic_constraints() { // closedness constraints
		cover_constraint();
		minFreq_constraint();
		closed_constraint();
	}
	
	/*
	public void diversity_constraint(BitSet pattern, double jmax) {
		System.out.println(pattern);
		int[] cov = cov_to_int(history_covers.get(pattern));
		int[] cste_1 = cste_1_diversity(jmax, cov); // H[q,t] - Jmax*(1-H[q,t])
		int cste_2 = cste_2_diversity(jmax, cov); // sum_t(Jmax*H[q,t])
		
		model.scalar(T, cste_1, "<=", cste_2).post();
	}
	//*/
	
	private int[] col_1(int i) {
		int[] col = new int[nbTransactions];
		for(int t=0; t<nbTransactions; t++) {
			col[t] = dataset[i].get(t) ? 1 : 0;
		}
		return col;
	}
	
	private int[] col_2(int c) {
		int[] col = new int[nbTransactions];
		for(int i=0; i<nbTransactions; i++) {
			col[i] = dataset[c].get(i) ? 0 : 1;
		}
		return col;
	}
	
	private int[][] rows(){ // get 1-r[i][j]
		int[][] r = new int[nbTransactions][nbItems];
		for(int i=0; i<nbTransactions; i++) {
			for(int j=0; j<nbItems; j++) {
				r[i][j] = dataset[j].get(i) ? 0 : 1;
			}
		}
		return r;
	}
	
	/*
	private int[] cov_to_int(BitSet cov) {
		int[] int_cov = new int[nbTransactions];
		for(int i=0; i<nbTransactions; i++) {
			int_cov[i] = cov.get(i) ? 1 : 0;
		}
		return int_cov;
	}
	
	private int[] cste_1_diversity(double jmax, int[] cov) {
		//int jm = (int) Math.round(jmax*100);
		int[] cste = new int[nbTransactions];
		
		for(int t=0; t<nbTransactions; t++) {
			cste[t] = (int) Math.round(
					(cov[t] - jmax*(1-cov[t]))*100);
		}
		return cste;
	}
	
	private int cste_2_diversity(double jmax, int[] cov) {
		int cste = 0;
		int jm = (int) Math.round(jmax*100);
		
		for(int i=0; i<nbTransactions; i++) {
			cste += cov[i];
		}
		return jm*cste;
	}
	//*/
	
	public void set_history_cover(BitSet pattern) {
		BitSet cov = covers.getCoverPOP(new TItemSet(pattern)).getListTransactions();
		history_covers.put((BitSet)pattern.clone(), cov);
	}
	
	public void set_history_cover(BitSet pattern, BitSet cover) {
		history_covers.put((BitSet)pattern.clone(), (BitSet)cover.clone());
	}
	
	

}
