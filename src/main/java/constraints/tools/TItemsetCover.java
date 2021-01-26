package constraints.tools;

public class TItemsetCover {
	public TItemSet itemSet;
	public TTransactionSet transactions;
	
	public TItemsetCover(TItemSet itemset, TTransactionSet coverture) {
		this.itemSet = itemset;
		this.transactions = coverture;
	}
	
	public String toString() {
		StringBuilder string = new StringBuilder();
		string.append(itemSet);
		string.append(" ");
		string.append(transactions);
		return string.toString();
	}

	public TItemSet getItemSet() {
		return itemSet;
	}

	public TTransactionSet getTransactions() {
		return transactions;
	}
}
