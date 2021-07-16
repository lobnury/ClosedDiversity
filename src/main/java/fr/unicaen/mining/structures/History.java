package fr.unicaen.mining.structures;

import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;

public class History {

	private Solution[] allItemsets;
	private int nbElement;
	
	HashMap<BitSet, Double> itemsetJaccard = new HashMap<BitSet, Double>();

	public History() {
		allItemsets = null;
		this.nbElement = 0;
	}
	
	
	public void addSolutionCP(Solution s) {
		Solution[] hist = new Solution[nbElement + 1];
		for (int i = 0; i < nbElement; i++)
			hist[i] = allItemsets[i];
		hist[nbElement] = s;
		allItemsets = hist;
		nbElement++;
	}
	
	public void addSolutionCP(Solution s, BitSet cov) {
		s.setCovX(cov);
		Solution[] hist = new Solution[nbElement + 1];
		for (int i = 0; i < nbElement; i++)
			hist[i] = allItemsets[i];
		hist[nbElement] = s;
		allItemsets = hist;
		nbElement++;
	}

	public void addSolutionCD(Solution s, BitSet cov, int label) {
		s.setCovX(cov);
		s.setLabel(label);
		Solution[] hist = new Solution[nbElement + 1];
		for (int i = 0; i < nbElement; i++)
			hist[i] = allItemsets[i];
		hist[nbElement] = s;
		allItemsets = hist;
		nbElement++;
	}
	
	public Solution[] getAllItemsets() {
		return allItemsets;
	}
	
	public void setHistory(Solution[] history) {
		allItemsets = history;
	}
	
	public int getNbElement() {
		return nbElement;
	}
	
	public void setNbElement(int nbElement) {
		this.nbElement = nbElement;
	}
	
	public void printSolutions() {
		System.out.println("***** Solutions *****");
		System.out.print("-");
		for (int i = 0; i < nbElement; i++) {
			String st = " -";
			System.out.print(" " + allItemsets[i].getItemset() + st);
		}
		System.out.println("");

		System.out.print("-");
		for (int i = 0; i < nbElement; i++) {
			System.out.print(" " + allItemsets[i].getJaccard() + " -");
		}

		System.out.println("\n");
	}
	
	public void getCPSolutions(FileWriter fr) {
		for (int i = 0; i < nbElement; i++) {
			String s = "";
			BitSet h = allItemsets[i].getItemset();
			s += "[ ";
			for (int item = h.nextSetBit(0); item != -1; item = h.nextSetBit(item + 1)) {
				s += "" + (item + 1) + " ";
			}
			s += "] ";
			//s += "[ " + formatCover(allItemsets[i].getCovX()) + " ] ";
			s += "[  ]\n";
			
			try {
				fr.write(s);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Error occured when writing solutions.");
				e.printStackTrace();
			}
		}
	}
	
	public void getCPSolutionsWithCover(FileWriter fr) {
		for (int i = 0; i < nbElement; i++) {
			String s = "";
			BitSet h = allItemsets[i].getItemset();
			s += "[ ";
			for (int item = h.nextSetBit(0); item != -1; item = h.nextSetBit(item + 1)) {
				s += "" + (item + 1) + " ";
			}
			s += "] ";
			// s += "[ " + formatCover(allItemsets[i].getCovX()) + " ] ";
			s += "[  ] [  ]\n";
			try {
				fr.write(s);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Error occured when writing solutions.");
				e.printStackTrace();
			}
		}
	}
	
	public void getCDSolutionsWithCover(FileWriter fr) {
		for (int i = 0; i < nbElement; i++) {
			String s = "";
			BitSet h = allItemsets[i].getItemset();
			s += "[ " + allItemsets[i].getLabel() + " ] ";
			s += "[ ";
			for (int item = h.nextSetBit(0); item != -1; item = h.nextSetBit(item + 1)) {
				s += "" + (item + 1) + " ";
			}
			s += "] ";
			s += "[ " + formatCover(allItemsets[i].getCovX()) + " ] ";
			s += "[ 1.0 1.0 1.0 ]\n";
			try {
				fr.write(s);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Error occured when writing solutions.");
				e.printStackTrace();
			}
		}
	}
	
	public String getSolutions() {
		String s = "";
		for (int i = 0; i < nbElement; i++) {
			BitSet h = allItemsets[i].getItemset();
			for (int item = h.nextSetBit(0); item != -1; item = h.nextSetBit(item + 1)) {
				s += "" + (item + 1) + " ";
			}
			s += "\n";
		}
		return s;
	}
	
	private String formatCover(BitSet cov) {
		String s = "";
		for (int tr = cov.nextSetBit(0); tr != -1; tr = cov.nextSetBit(tr + 1)) {
			s += "" + (tr + 1) + " ";
		}
		if (!s.isEmpty())
			s = s.substring(0, s.length() - 1);
		return s;
	}
	
}
