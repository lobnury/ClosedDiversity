package fr.unicaen.mining.datasets;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import fr.unicaen.mining.structures.IncrementCovers;
import fr.unicaen.mining.structures.TItemSet;
import fr.unicaen.mining.structures.TTransactionSet;

public class DataSet {
	private BitSet[] VerticalDataBase;
	private List<TItemSet> HorizontalBase;

	public Set<Integer> uniqueItems = new HashSet<Integer>();
	private int maxItem = 0;
	private int minItem = Integer.MAX_VALUE;
	int i = 0;
	TTransactionSet AllTransactions;
	public IncrementCovers covers;
	public IncrementCovers coversBorne;
	
	public DataSet(String DataObjectSetPath) throws IOException {
		covers = new IncrementCovers(this);
		coversBorne = new IncrementCovers(this);
		
		HorizontalBase = new ArrayList<TItemSet>();
		BufferedReader br = new BufferedReader(new FileReader(DataObjectSetPath));
		String items;
		while ((items = br.readLine()) != null) { // iterate over the lines to build the transaction
			if (items.equals("[EOF]"))
				break;
			// if the line is a comment, is empty or is metadata
			if (items.isEmpty() == true || items.charAt(0) == '#' || items.charAt(0) == '%' || items.charAt(0) == '@') {
				continue;
			}

			HorizontalBase.add(createTransaction(items));
		}
		br.close();
		
		////////////////////////////////////// vertical
		////////////////////////////////////// representation///////////////////////
		
		VerticalDataBase = new BitSet[getNbrVar()];
		for (int item = 0; item < getNbrVar(); item++) {
			VerticalDataBase[item] = new BitSet();
		}

		BitSet Transactions = new BitSet();
		for (i = 0; i < HorizontalBase.size(); i++) {
			TItemSet itemSet = HorizontalBase.get(i);
			Transactions.set(i);
			
			for (Integer item : itemSet.getListItems()) {
				// for each item get its bucket and add the current transaction

				if (minItem == 1) {
					VerticalDataBase[item - 1].set(i);

				} else {
					VerticalDataBase[item - 1].set(i);
				}
			}
		}
		
		AllTransactions = new TTransactionSet(Transactions);
	}

	public List<String> getTokensWithCollection(String str) {
		return Collections.list(new StringTokenizer(str, " ")).stream().map(token -> (String) token)
				.collect(Collectors.toList());
	}

	// ------ MinArray function
	public Integer minArrayListComparator(List<Integer> listOfIntegers) {
		return listOfIntegers.stream().mapToInt(v -> v).min().orElseThrow(NoSuchElementException::new);
	}

	// ------ MaxArray function
	public Integer maxArrayListComparator(List<Integer> listOfIntegers) {
		return listOfIntegers.stream().mapToInt(v -> v).max().orElseThrow(NoSuchElementException::new);
	}

	// -------------------------

	/**
	 * Create a transaction object from a line from the input file
	 * 
	 * @param line
	 *            a line from input file
	 * @return a transaction
	 */
	private TItemSet createTransaction(String line) {
		List<Integer> itemsSorted = new ArrayList<Integer>();
		getTokensWithCollection(line).forEach(elt -> {
			itemsSorted.add(Integer.parseInt(elt));
			uniqueItems.add(Integer.parseInt(elt));
		});
		
		int lastItem = maxArrayListComparator(itemsSorted);
		if (lastItem > maxItem) {
			maxItem = lastItem;
		}
		
		int firstItem = minArrayListComparator(itemsSorted);
		if (minItem > firstItem) {
			minItem = firstItem;
		}
		return new TItemSet(itemsSorted);
	}

	public List<TItemSet> getObjectTransactions() {
		return HorizontalBase;
	}

	public Set<Integer> getUniqueItems() {
		return uniqueItems;
	}

	public int getMaxItem() {
		return maxItem;
	}

	public int getNbrVar() {
//		return nbrVar;
		return maxItem;
	}

	public int getTransactionsSize() {
		return HorizontalBase.size();
	}

	@Override
	public String toString() {
		StringBuilder DataObjectSetContent = new StringBuilder();

		for (TItemSet transaction : HorizontalBase) {
			DataObjectSetContent.append(transaction);
			DataObjectSetContent.append("\n");
		}
		return DataObjectSetContent.toString();
	}

	public BitSet[] getVerticalDataBase() {
		return VerticalDataBase;
	}

	public List<TItemSet> getHorizontalDataBase() {
		return HorizontalBase;
	}

	public TTransactionSet getAllTransactions() {
		return AllTransactions;
	}

	public IncrementCovers getCovers() {
		return covers;
	}

	public void setCovers(IncrementCovers covers) {
		this.covers = covers;
	}
	
	public IncrementCovers getCoversBorne() {
		return coversBorne;
	}

	public void setCoversBorne(IncrementCovers coversBorne) {
		this.coversBorne = coversBorne;
	}
	
}
