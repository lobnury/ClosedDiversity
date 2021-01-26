package constraints.tools;

import java.util.BitSet;

public class TTransactionSet {
	private BitSet ListTransaction;
	
	public TTransactionSet() {
		this.ListTransaction = new BitSet();
	}
	
	public TTransactionSet(BitSet list) {
		this.ListTransaction = (BitSet) list.clone();
	}
	
	public BitSet getListTransactions() {
		return (BitSet) ListTransaction.clone();
	}
	
	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		string.append(ListTransaction);
		string.append(" ");
		return string.toString();
	}

}
