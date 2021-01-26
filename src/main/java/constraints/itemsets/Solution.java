package constraints.itemsets;

import java.util.BitSet;

public class Solution {
	
	private BitSet itemset;
	private BitSet covX;
	private double jaccard;
	private int label;
	
	public Solution(BitSet itemset, double jaccard) {
		this.itemset = (BitSet) itemset.clone();
		this.jaccard = jaccard;
		this.covX = new BitSet();
		label = 0;
	}
	
	public Solution(BitSet itemset) {
		this.itemset = (BitSet) itemset.clone();
		this.jaccard = 1;
		this.covX = new BitSet();
		label = 0;
	}
	
	public Solution(BitSet itemset, BitSet covX) {
		this.itemset = (BitSet) itemset.clone();
		this.covX = covX;
		this.jaccard = 1;
		this.covX = new BitSet();
		label = 0;
	}
	
	public Solution(BitSet itemset, BitSet covX, int representant, int label) {
		this.itemset = (BitSet) itemset.clone();
		this.covX = covX;
		this.jaccard = 1;
		this.covX = new BitSet();
		this.label = label;
	}
	
	public Solution(Solution s) {
		this.itemset = (BitSet) s.getItemset().clone();
		this.jaccard = s.getJaccard();
		this.covX = (BitSet) s.getCovX().clone();
		label = s.getLabel();
	}

	public BitSet getItemset() {
		return (BitSet) itemset.clone();
	}

	public void setItemset(BitSet itemset) {
		this.itemset = (BitSet) itemset.clone();
	}

	public double getJaccard() {
		return jaccard;
	}

	public void setJaccard(double jaccard) {
		this.jaccard = jaccard;
	}

	public BitSet getCovX() {
		return covX;
	}

	public void setCovX(BitSet covX) {
		this.covX = (BitSet) covX.clone();
	}

	public int getLabel() {
		return label;
	}

	public void setLabel(int label) {
		this.label = label;
	}
	
}
