package constraints.closedpattern;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

import constraints.dataset.DataSet;
import constraints.tools.TItemSet;
import constraints.tools.TTransactionSet;

public final class ClosedPatterns extends Propagator<BoolVar> {
	BoolVar[] p;
	DataSet dataset;
	int minFreq;
	//IncrementCovers covers;

	public ClosedPatterns(DataSet dataset0, int minFreq0, BoolVar[] p) {
		super(p);
		this.p = p;
		this.dataset = dataset0;
		this.minFreq = minFreq0;
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		// handle free items and their cover wrt cov_X
		HashMap<Integer, TTransactionSet> FreeItemsCover = new HashMap<Integer, TTransactionSet>();
		
		// internal structures
		BitSet free_items = new BitSet(); // free items
		BitSet filtered_items = new BitSet(); // items set to 0
		BitSet current_itemset_items = new BitSet(); // items set to 1 (current closed pattern)
		BitSet Sigma_libNeg = new BitSet();
		BitSet Sigma_libPos = new BitSet();
		
		for (int item = 0; item < dataset.getNbrVar(); item++) {
			if (p[item].isInstantiatedTo(0)) {
				filtered_items.set(item);
			} else if (p[item].isInstantiatedTo(1)) {
				current_itemset_items.set(item);
			} else 
				free_items.set(item);
		}
		// cov(X)
		TTransactionSet coverPos1 = dataset.getCovers().getCoverPOP(new TItemSet(current_itemset_items));
		
		for (int item = free_items.nextSetBit(0); item != -1; item = free_items.nextSetBit(item + 1)) {
			if (dataset.getCovers().checkSameCover(coverPos1, item)) { // full-extension
				p[item].removeValue(0, Cause.Null);
				current_itemset_items.set(item);
				dataset.getCovers().pushCover(new TItemSet(current_itemset_items), coverPos1);
				Sigma_libPos.set(item); // <---

			} else {
				TTransactionSet projection = dataset.getCovers().intersectCover(coverPos1, item); 
				// frequency filtering
				if (projection.getListTransactions().cardinality() < minFreq) {
					p[item].removeValue(1, Cause.Null);
					Sigma_libNeg.set(item); // <----

				} else {
					// if the item is not filtered neither by the frequency rule nor set to 1 by full extemsion
					FreeItemsCover.put(item, projection);
				}
			}
		}

		free_items.andNot(Sigma_libPos);
		free_items.andNot(Sigma_libNeg);
		
		if (!free_items.isEmpty()) {
			for (int i = filtered_items.nextSetBit(0); i != -1; i = filtered_items.nextSetBit(i + 1)) {
				TTransactionSet cover01 = dataset.getCovers().intersectCover(coverPos1, i); 

				for (int j = free_items.nextSetBit(0); j != -1; j = free_items.nextSetBit(j + 1)) {
					TTransactionSet cover02 = FreeItemsCover.get(j);
					if (coverInclusion(cover01, cover02))
						p[j].removeValue(1, Cause.Null);
				}
			}
		}
	}

	@Override
	public ESat isEntailed() {
		return ESat.TRUE;
	}

	public boolean condition(TTransactionSet coverPos1, List<Integer> Sigma_ne) {
		for (Integer item : Sigma_ne) {
			if (dataset.getCovers().checkSameCover(coverPos1, item))  
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

}
