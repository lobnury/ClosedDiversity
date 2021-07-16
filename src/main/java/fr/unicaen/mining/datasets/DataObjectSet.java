package fr.unicaen.mining.datasets;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import fr.unicaen.mining.structures.ObjectTransaction;;

/**
 * This is the parser class for the DataObjectSet. It has actions related to
 * parse a txt based file to a Dataset class.
 *
 * @see AlgoLCM
 * @author Alan Souza <apsouza@inf.ufrgs.br>
 */
public class DataObjectSet {
	static List<Integer>[] VerticalDataBase;
	static List<ObjectTransaction>[] TransacationBase;
	private static List<ObjectTransaction> ObjectTransactions;
	private Integer[] ObjectTransactionsItems;

	public static Set<Integer> uniqueItems = new HashSet<Integer>();

	private static int maxItem = 0;

	@SuppressWarnings("unchecked")
	public DataObjectSet(String DataObjectSetPath) throws IOException {

		ObjectTransactions = new ArrayList<ObjectTransaction>();

		BufferedReader br = new BufferedReader(new FileReader(DataObjectSetPath));
		String items;
		while ((items = br.readLine()) != null) { // iterate over the lines to build the transaction
			// if the line is a comment, is empty or is metadata
			if (items.isEmpty() == true || items.charAt(0) == '#' || items.charAt(0) == '%' || items.charAt(0) == '@') {
				continue;
			}

			getObjectTransactions().add(createTransaction(items));
		}
		br.close();

		/// sort transactions by increasing last item (optimization)
		Collections.sort(ObjectTransactions, new Comparator<ObjectTransaction>() {
			public int compare(ObjectTransaction arg0, ObjectTransaction arg1) {
				return arg0.getItems()[arg0.getItems().length - 1] - arg1.getItems()[arg1.getItems().length - 1];
			}
		});

		// create the list of items in the database and sort it
		ObjectTransactionsItems = new Integer[uniqueItems.size()];
		int i = 0;
		for (Integer item : uniqueItems) {
			ObjectTransactionsItems[i++] = item;
		}
		Arrays.sort(ObjectTransactionsItems);
		
		//vertical representation
		VerticalDataBase = new List[getMaxItem() + 1];
		TransacationBase = new List[getSize()];
			for (int item=0;item<getMaxItem() + 1;item++) {	
			VerticalDataBase[item] = new ArrayList<Integer>();
		}

		for ( i = 0; i < getSize(); i++) {
			TransacationBase[i] = new ArrayList<ObjectTransaction>();
		}
		int o = 0;
		for (ObjectTransaction ObjectTransaction : getObjectTransactions()) {
			TransacationBase[o].add(ObjectTransaction);
			o++;
		}
		for ( i = 0; i <getSize(); i++) {

			ObjectTransaction ObjectTransaction = TransacationBase[i].get(0);

			for (Integer item : ObjectTransaction.getItems()) {
				// for each item get its bucket and add the current transaction
				VerticalDataBase[item].add(i);
			}

		}
	
	}

	/**
	 * Create a transaction object from a line from the input file
	 * 
	 * @param line
	 *            a line from input file
	 * @return a transaction
	 */
	private ObjectTransaction createTransaction(String line) {

		///////////////////////////////////////////// build the items//////////////////////////////////////////
		Pattern splitPattern = Pattern.compile(" ");
		String[] items = splitPattern.split(line);

		Integer[] itemsSorted = new Integer[items.length];

		for (int i = 0; i < items.length; i++) {
			Integer item = Integer.valueOf(items[i]);
			itemsSorted[i] = item;

			uniqueItems.add(item);
		}

		/////////////////////////////// update max item by checking the last item of the transaction//////////////////////////
		int lastItem = itemsSorted[itemsSorted.length - 1];
		if (lastItem > maxItem) {
			maxItem = lastItem;
		}
		return new ObjectTransaction(itemsSorted);
	}

	public  List<ObjectTransaction> getObjectTransactions() {
		return ObjectTransactions;
	}

	public Set<Integer> getUniqueItems() {
		return uniqueItems;
	}
	
	public static int getMaxItem() {
		return maxItem;
	}
	public static int getSize() {
		return ObjectTransactions.size() ;
	}

	@Override
	public String toString() {
		StringBuilder DataObjectSetContent = new StringBuilder();

		for (ObjectTransaction transaction : ObjectTransactions) {
			DataObjectSetContent.append(transaction);
			DataObjectSetContent.append("\n");
		}
		return DataObjectSetContent.toString();
	}

	public  List<Integer>[] getVerticalDataBase() {
		return VerticalDataBase;
	}

	public List<ObjectTransaction>[] getTransacationBase() {
		return TransacationBase;
	}
}
