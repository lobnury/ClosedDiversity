/*
 * Copyright Vladimir Dzyuba
 * From Implementation of the Flexics pattern sampler.
 * Dzyuba, V., van Leeuwen, M., & De Raedt, L. (2016). Flexible constrained sampling with guarantees for pattern mining. arXiv:1610.09263.
*/

package fr.unicaen.mining.util;

import java.util.LinkedList;

import org.apache.commons.math3.fraction.Fraction;

public class Matrix {
	Fraction[][] matrice; // lobnury
	int numRows;
	int numCols;

	static class Coordinate {
		int row;
		int col;

		Coordinate(int r, int c) {
			row = r;
			col = c;
		}

		public String toString() {
			return "(" + row + ", " + col + ")";
		}
	}

	Matrix(double[][] m) {
		numRows = m.length;
		numCols = m[0].length;
		matrice = new Fraction[numRows][numCols]; // lobnury

		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++)
				matrice[i][j] = new Fraction(m[i][j]); // lobnury
		}
	}
	
	Matrix(int[][] m) {
		numRows = m.length;
		numCols = m[0].length;
		matrice = new Fraction[numRows][numCols]; // lobnury

		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++)
				matrice[i][j] = new Fraction(m[i][j]); // lobnury
		}
	}

	public void Interchange(Coordinate a, Coordinate b) {
		//////////////////////////////////////// lobnury
		Fraction[] tempo = new Fraction[numCols];
		for (int j = 0; j < numCols; j++)
			tempo[j] = matrice[a.row][j];
		for (int j = 0; j < numCols; j++) {
			matrice[a.row][j] = matrice[b.row][j];
			matrice[b.row][j] = tempo[j];
		}
		////////////////////////////////////////////////

		int t = a.row;
		a.row = b.row;
		b.row = t;
	}

	public void Scale(Coordinate x, Fraction d) {
		//////////////////////////////////////// lobnury
		Fraction[] tempo = new Fraction[numCols];
		for (int j = 0; j < numCols; j++)
			tempo[j] = matrice[x.row][j];

		for (int j = 0; j < numCols; j++)
			matrice[x.row][j] = tempo[j].multiply(d);
		////////////////////////////////////////////////
	}

	public void MultiplyAndAdd(Coordinate to, Coordinate from, Fraction scalar) {
		//////////////////////////////////////// lobnury
		Fraction[] tempo_r = new Fraction[numCols];
		Fraction[] tempo_rm = new Fraction[numCols];
		for (int j = 0; j < numCols; j++) {
			tempo_r[j] = matrice[to.row][j];
			tempo_rm[j] = matrice[from.row][j];
		}

		for (int j = 0; j < numCols; j++) {
			matrice[to.row][j] = tempo_r[j].add((tempo_rm[j].multiply(scalar)));
		}
		////////////////////////////////////////////////

	}

	public void echelonize() {
		Coordinate pivot = new Coordinate(0, 0);
		
		int submatrix = 0;
		for (int x = 0; x < numCols; x++) {
			pivot = new Coordinate(pivot.row, x);
			for (int i = x; i < numCols; i++) {
				if (isColumnZeroes(pivot) == false)
					break;
				else
					pivot.col = i;
			}
			pivot = findPivot(pivot);

			if (getCoordinate(pivot).doubleValue() == 0.0) {
				pivot.row++;
				continue;
			}

			if (pivot.row != submatrix)
				Interchange(new Coordinate(submatrix, pivot.col), pivot);

			if (getCoordinate(pivot).doubleValue() != 1) {
				Fraction scalar = getCoordinate(pivot).reciprocal();
				Scale(pivot, scalar);
			}

			for (int i = pivot.row; i < numRows; i++) {
				if (i == pivot.row)
					continue;
				
				Coordinate belowPivot = new Coordinate(i, pivot.col);
				Fraction complement = (getCoordinate(belowPivot).negate().divide(getCoordinate(pivot)));
				MultiplyAndAdd(belowPivot, pivot, complement);
			}

			for (int i = pivot.row; i >= 0; i--) {
				if (i == pivot.row) {
					if (getCoordinate(pivot).doubleValue() != 1.0) 
						Scale(pivot, getCoordinate(pivot).reciprocal());
					continue;
				}
				if (i == pivot.row)
					continue;

				Coordinate abovePivot = new Coordinate(i, pivot.col);
				Fraction complement = (getCoordinate(abovePivot).negate().divide(getCoordinate(pivot)));
				MultiplyAndAdd(abovePivot, pivot, complement);
			}

			if ((pivot.row + 1) >= numRows || 
					isRowZeroes(new Coordinate(pivot.row + 1, pivot.col)))
				break;

			submatrix++;
			pivot.row++;
		}
		
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				if(matrice[i][j].intValue() < 0)
					matrice[i][j].negate();
			}
		}
	}

	public boolean isColumnZeroes(Coordinate a) {
		/////////////////////////////////////////////////// lobnury
		for (int i = 0; i < numRows; i++) {
			if (matrice[i][a.col].doubleValue() != 0.0)
				return false;
		}
		///////////////////////////////////////////////////////////
		return true;
	}

	public boolean isRowZeroes(Coordinate a) {
		/////////////////////////////////////////////////// lobnury
		for (int i = 0; i < numCols; i++) {
			if (matrice[a.row][i].doubleValue() != 0.0)
				return false;
		}
		///////////////////////////////////////////////////////////

		return true;
	}

	public Coordinate findPivot(Coordinate a) {
		int first_row = a.row;
		Coordinate pivot = new Coordinate(a.row, a.col);
		Coordinate current = new Coordinate(a.row, a.col);

		for (int i = a.row; i < (numRows - first_row); i++) {
			current.row = i;
			if (getCoordinate(current).doubleValue() == 1.0)
				Interchange(current, a);
		}

		current.row = a.row;
		for (int i = current.row; i < (numRows - first_row); i++) {
			current.row = i;
			if (getCoordinate(current).doubleValue() != 0) {
				pivot.row = i;
				break;
			}
		}
		return pivot;
	}

	public Fraction getCoordinate(Coordinate a) {
		return matrice[a.row][a.col]; // lobnury
	}

	public String toString() {
		// lobnury
		String s = "";
		for (int i = 0; i < numRows; i++) {
			s += "[";
			for (int j = 0; j < numCols; j++)
				s += " " + matrice[i][j];
			s += " ]\n";
		}
		return s;
	}

	public static void main(String[] args) {
		double test_1[][] = { 
				{ 1, 0, 0, 0, 0, 1, 1 }, 
				{ 0, 1, 0, 1, 1, 1, 0 }, 
				{ 1, 1, 0, 1, 0, 1, 0 },
				{ 0, 1, 0, 0, 1, 1, 1 }, 
				{ 0, 2, 0, 0, 2, 2, 2 }, 
				{ 0, 0, 0, 0, 0, 0, 0 }, 
				{ 0, 0, 0, 0, 0, 0, 0 }
		};
		
		double test_2[][] = { 
				{ 1, 0, 0, 0, 1, 1 }, 
				{ 0, 1, 1, 1, 1, 0 }, 
				{ 1, 1, 1, 0, 1, 0 }, 
				{ 0, 1, 0, 1, 1, 1 },
				{ 0, 2, 0, 2, 2, 2 }, 
				{ 0, 0, 0, 0, 0, 0 } 
		};
		
		double test_3[][] = { 
				{ 1, 0, 0, 0, 1, 1 }, 
				{ 0, 1, 1, 1, 1, 0 }, 
				{ 1, 1, 1, 0, 1, 0 }, 
				{ 0, 1, 0, 1, 1, 1 } 
		};
		int test_4[][] = {
				{ 1, 0, 0, 0, 0, 1, 1 }, 
				{ 0, 1, 0, 1, 1, 1, 0 }, 
				{ 1, 1, 0, 1, 0, 1, 0 },
				{ 0, 1, 0, 0, 1, 1, 1 }
		};

		Matrix m = new Matrix(test_4);
		System.out.println("before\n" + m.toString() + "\n");
		m.echelonize();
		System.out.println("after\n" + m.toString() + "\n");
	}

}
