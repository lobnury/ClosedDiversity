package fr.unicaen.mining.util;

import static org.chocosolver.solver.search.strategy.Search.intVarSearch;

import java.util.BitSet;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.variables.IntVar;

import fr.unicaen.mining.datasets.DataSet;
import fr.unicaen.mining.RunMain;
import fr.unicaen.mining.models.closeddiversity.ClosedDiversity;
import fr.unicaen.mining.structures.Solution;
import fr.unicaen.mining.structures.TItemSet;
import fr.unicaen.mining.structures.TTransactionSet;

public class ClosedDiversityStrategies {
	public ClosedDiversityStrategies() {
		
	}
	
	public static void defineMincovSearchStrategy(Model model, ClosedDiversity cd, IntVar... vars) {
		model.getSolver().setSearch(intVarSearch(
				(VariableSelector<IntVar>) variables -> selectMincovVar(model, cd, vars), 
				(IntValueSelector) var -> var.getUB(), 
				vars));
	}
	
	public static void defineWitnessSearchStrategy(Model model, ClosedDiversity cd, IntVar... vars) {
		model.getSolver().setSearch(
				intVarSearch((VariableSelector<IntVar>) variables -> selectWitnessVar(model, cd, vars), 
				(IntValueSelector) var -> var.getUB(), 
				vars));
	}
	
	public static IntVar selectMincovVar(Model model, ClosedDiversity cd, IntVar... vars) {
		//IntVar var_i = null;
		int item = -1;
		if(cd.nextVar != -1) {
			// Estimated Frequencies
			item = cd.nextVar;
			//var_i = vars[item];
			RunMain.currentItem = item;
		}
		else {
			int i = 0;
			for (IntVar v : vars) {
				if (!v.isInstantiated()) {
					//var_i = v;
					item = i;
					RunMain.currentItem = item;
					break;
				}
				i++;
			}
		}
		if(item != -1)
			return vars[item];
		return null;
	}
	public static IntVar selectWitnessVar(Model model, ClosedDiversity cd, IntVar... vars) {
		//IntVar var_i = null;
		int item = -1, i = 0;
		
		// we leave a witness node
		if((RunMain.witnessStrategy.equals("WITNESSDIVSOL")) && 
				(RunMain.witnessItem != -1) && 
				vars[RunMain.witnessItem].isInstantiatedTo(0)) {
			RunMain.isWitness = false;
			RunMain.witnessItem = -1;
		}
		
		// we explore a non witness node
		if(!RunMain.isWitness && (RunMain.witnessItem == -1)) {
			if(cd.nbSolutions > 0) {
				TTransactionSet covPos1 = new TTransactionSet(cd.cov);
				for (IntVar v : vars) {
					if (!v.isInstantiated()) {
						BitSet cov_XUx = (BitSet) cd.dataset.covers.intersectCover(
								covPos1,i).getListTransactions();
						Double current_ub = (double) 1;
						if(patternGrowth_UB(cd.dataset, cd.history_itemsets, covPos1.getListTransactions(), 
								cov_XUx, current_ub, i, cd.seuil, cd.nbSolutions)) {
							item = i;
							break;
						}
					}
					i++;
				}
			}
			if(item != -1) {
				RunMain.isWitness = true; // desactivation du calcul de LB
				//var_i = vars[item]; // branch on witness items
				//nbWitnessNode++;
				if(RunMain.witnessStrategy.equals("WITNESSDIVSOL")) {
					RunMain.witnessItem = item;
				}
			}
			else if(cd.nextVar != -1) {
				// Estimated Frequencies
				item = cd.nextVar;
				//var_i = vars[item];
			}
			RunMain.currentItem = item;
		}
		else {
			if(cd.nextVar != -1) {
				// Estimated Frequencies
				item = cd.nextVar;
				//var_i = vars[item];
				RunMain.currentItem = item;
			}
			else {
				i = 0;
				for (IntVar v : vars) {
					if (!v.isInstantiated()) {
						//var_i = v;
						item = i;
						RunMain.currentItem = item;
						break;
					}
					i++;
				}
			}
		}
		
		// model.getSolver().setSearch(var_i, var_i.getUB(), vars);
		
		if(item != -1)
			return vars[item];
		return null;
				
	}
	
	// lobnury
	//public static boolean patternGrowth_UB(DataSet dataset, BitSet[] history, BitSet covX, 
	public static boolean patternGrowth_UB(DataSet dataset, Solution[] history, BitSet covX, 
			BitSet cov_XUx, Double maxUB, int item, int minseuil, int nbSolutions) {
		boolean grow = true;
		grow = growth_UB(dataset, history, covX, cov_XUx, maxUB, item, minseuil, nbSolutions);
		return grow;
	}

	// lobnury
	//public static boolean growth_UB(DataSet dataset, BitSet[] history, BitSet covX, 
	public static boolean growth_UB(DataSet dataset, Solution[] history, BitSet covX, 
			BitSet cov_XUx, Double maxUB, int item, int minseuil, int nbSolutions) {
		boolean growth_ub = true;
		maxUB = 0.0;
		for(int i = 0; i < nbSolutions; i++) {
			// list of transactions covered by history itemSet Hi
			BitSet cov_Hi = new BitSet();
			cov_Hi = dataset.coversBorne.getCoverPOP(
					new TItemSet(history[i].getItemset())).getListTransactions();
			BitSet cov_intersect = (BitSet) cov_XUx.clone();
			cov_intersect.and(cov_Hi);
			
			double ub_jaccard = 0.0;
			BitSet covP_Hi = (BitSet) cov_Hi.clone();
			covP_Hi.andNot(cov_XUx);
			
			if(cov_intersect.cardinality() < minseuil) {
				ub_jaccard = ((double) cov_intersect.cardinality()) / 
						((double) (minseuil + covP_Hi.cardinality()));
			}
			else {
				ub_jaccard = ((double) cov_intersect.cardinality()) / 
						((double) (cov_intersect.cardinality() + covP_Hi.cardinality()));
			}
			if((ub_jaccard > ClosedDiversity.jMax) || 
					((ub_jaccard == ClosedDiversity.jMax) && (ub_jaccard == 0)) || 
					((ub_jaccard == ClosedDiversity.jMax) && (ub_jaccard == 1))) {
				growth_ub = false;
				break;
			}
		}
		return growth_ub;
	}
}
