package constraints.tools;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;

import constraints.dataset.DataSet;

public class IncrementCovers {
	private DataSet dataSet;
	private Deque<TItemsetCover> CurrentCovers;

	public IncrementCovers(DataSet data1) {
		this.dataSet = data1;
		CurrentCovers = new ArrayDeque<TItemsetCover>();
	}

	public void pushCover(TItemSet itemSet0, TTransactionSet transactionSet0) {
		CurrentCovers.push(new TItemsetCover(itemSet0, transactionSet0));
	}

	public synchronized TTransactionSet getCoverPOP(TItemSet itemSet0) {
		TTransactionSet tr0 = null;
		boolean notFound0 = true;
		if (!itemSet0.getBitItemSet().isEmpty()) {
			while (notFound0 && (!CurrentCovers.isEmpty())) {
				TItemsetCover topObject0 = CurrentCovers.peek();
				if (topObject0.itemSet.isEqualItemSet(itemSet0)) {
					// Check if the @param itemset is at the top of the stack
					tr0 = new TTransactionSet(topObject0.transactions.getListTransactions());
					notFound0 = false;
					return tr0;
				} 
				else if (itemSet0.subItemSet(topObject0.itemSet)) {
					// if top itemset is a subset of the @param itemset
					TTransactionSet list = new TTransactionSet();
					BitSet DifItemSet = new BitSet();
					DifItemSet = (BitSet) itemSet0.getBitItemSet().clone();
					DifItemSet.andNot(topObject0.itemSet.getBitItemSet());
					list = new TTransactionSet(topObject0.transactions.getListTransactions());
					for (int item = DifItemSet.nextSetBit(0); item != -1; item = DifItemSet.nextSetBit(item + 1))
					 {
						list = getIntersection(list, new TTransactionSet(dataSet.getVerticalDataBase()[item]));
					}
				
					
					tr0 = list;

					pushCover(itemSet0, tr0);
					notFound0 = false;
					return tr0;
				} else
					CurrentCovers.pop();
			}
			
			// if the itemset's cover is not found (have not been store), we calculate it 
			if (notFound0) {
				TTransactionSet Tran = new TTransactionSet();
				int item0 = itemSet0.getBitItemSet().nextSetBit(0);
				Tran = new TTransactionSet(dataSet.getVerticalDataBase()[item0]);
				for (int item = 0; item < itemSet0.getBitItemSet().length(); item++) {
					if (itemSet0.getBitItemSet().get(item) == true) {
						Tran = getIntersection(Tran, new TTransactionSet(dataSet.getVerticalDataBase()[item]));
					}
				}
				tr0 = Tran;
				pushCover(itemSet0, tr0);
				return tr0;
			}

		} else {
			tr0 = dataSet.getAllTransactions();
			return tr0;
		}
		return tr0;
	}
	/**
	 * @param coverPos , ValeurItem.
	 * @return true if the 2 covers are the same otherwise false. 
	 */
	public boolean checkSameCover(TTransactionSet coverPos, Integer ValeurItem) {
		TTransactionSet Cover = new TTransactionSet();

		Cover = getIntersection(coverPos, new TTransactionSet(dataSet.getVerticalDataBase()[ValeurItem]));
		return (sameTransacation(coverPos.getListTransactions(), Cover.getListTransactions()));
	}
	/**
	 * @param coverture courante , itemvalue
	 * @return intersection of coverPos and itemvalue 
	 */
	public synchronized TTransactionSet intersectCover(TTransactionSet coverPos, Integer itemvalue) {
		BitSet T1 = new BitSet();
		BitSet T2 = new BitSet();
		T1 = (BitSet) coverPos.getListTransactions().clone();
		T2 = (BitSet) dataSet.getVerticalDataBase()[itemvalue];
		T1.and(T2);
		return new TTransactionSet(T1);

	}
	/**
	 * @param cover1, cover2
	 * @return the intersection of the 2 covers
	 */
	public TTransactionSet getIntersection(TTransactionSet Cover1, TTransactionSet Cover2) {
		BitSet T1 = new BitSet();
		BitSet T11 = new BitSet();
		T1 = Cover1.getListTransactions();
		T11 = (BitSet) T1.clone();
		T11.and(Cover2.getListTransactions());
		return new TTransactionSet(T11);
	};
	
	// check if the 2 covers are the same
	public boolean sameTransacation(BitSet Cover1, BitSet Cover2) {
		return (Cover1.equals(Cover2));
	}

}
