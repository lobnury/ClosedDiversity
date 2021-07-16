package fr.unicaen.mining.models.xor;

import java.util.BitSet;

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

import be.kuleuven.m4ri.DenseMatrixGF2;

public class ConstraintXOR extends Propagator<BoolVar> {
	int n, m;
	public BoolVar[] p;
	public int[][] all_coefficients_tab;
	public int[][] matrice;
	public DenseMatrixGF2 mat_m;
	public IStateInt[][] mat_m_copy;
	boolean reset = false;
	
	public ConstraintXOR(BoolVar[] p, int[][] coefficients_tab) {
		super(p);
		this.p = p;
		this.m = this.p.length;
		this.n = coefficients_tab.length;
		
		this.matrice = new int[this.n][this.m+1];
		this.all_coefficients_tab = new int[this.n][this.m+1];
		
		mat_m_copy = new IStateInt[n][m+1];
		mat_m = DenseMatrixGF2.random(n, m+1);
		for(int i=0; i<this.n; i++) {
			for(int j=0; j<= this.m; j++) {
				mat_m.set(i, j, coefficients_tab[i][j]);
				mat_m_copy[i][j] = 
						this.model.getEnvironment().makeInt(coefficients_tab[i][j]);
			}
		}
	}

	@Override
	public ESat isEntailed() {
		// TODO Auto-generated method stub
		return ESat.TRUE;
		// return null;
	}

	@Override
	public void propagate(int arg0) throws ContradictionException {
		// TODO Auto-generated method stub
		propagate_several_XORs();
	}
	
	public void propagate_several_XORs() throws ContradictionException {
		BitSet s_pos = new BitSet(), s_neg = new BitSet(), s_lib = new BitSet();
		for (int item = 0; item < p.length; item++) {
			if (p[item].isInstantiatedTo(1))
				s_pos.set(item);
			else if (p[item].isInstantiatedTo(0))
				s_neg.set(item);
			else
				s_lib.set(item);
		}
		
		if(reset) {
			reset = false;
			for(int i=0; i<this.n; i++) {
				for(int j=0; j<= this.m; j++) {
					mat_m.set(i, j, mat_m_copy[i][j].get());
				}
			}
		}
		
		boolean all_items_fixed = update_matrix();
		
		if(check_inconsistent_rows()) {
			reset = true;
			fails();
		}
		else if(all_items_fixed) {
			reset = true;
		}
		else {
			mat_m.echelonize(true);
			for(int i=0; i<this.n; i++) {
				for(int j=0; j<= this.m; j++) {
					mat_m_copy[i][j].set(mat_m.get_bit_val(i, j));
				}
			}
			
			if(check_inconsistent_rows()) {
				reset = true;
				fails();
			}
			fix_variables();
		}
	}
	
	public void reset() {
		for(int i=0; i<this.n; i++) {
			for(int j=0; j<= this.m; j++) {
				mat_m.set(i, j, matrice[i][j]);
			}
		}
	}
	
	public void flip_bit(int row, int col) {
		int current_val = mat_m.get_bit_val(row, col);
		int new_val = current_val > 0 ? 0 : 1;
		mat_m.set(row, col, new_val);
		
		mat_m_copy[row][col].set(new_val);
	}
	
	public boolean update_matrix() {
		boolean all_items_fixed = true;
		for(int i = 0; i < m; i++) { // m : nombre d'item
			if(p[i].isInstantiated()) {
				//*
				for(int c = 0; c < n; c++) { // n : nombre de contraintes XOR
					if((mat_m.get_bit_val(c, i) == 1) && (p[i].isInstantiatedTo(1))) {
						flip_bit(c, m);
					}
					mat_m.set(c, i, 0);
					mat_m_copy[c][i].set(0);
				}
				
			}
			else
				all_items_fixed = false;
		}
		return all_items_fixed;
	}
	
	public boolean check_inconsistent_rows() {
		for(int c = 0; c < n; c++) {
			if(mat_m.get_bit_val(c, m) == 1) { // bit de parité
				boolean row_still_has_ones = false;
				for(int i = 0; i < m; i++) {
					if(mat_m.get_bit_val(c, i) == 1) {
						row_still_has_ones = true;
						break;
					}
				}
				if(!row_still_has_ones)
					return true;
			}
		}
		return false;
	}
	
	public void fix_variables() {
		for(int c = 0; c < n; c++) {
			int vars_in_xor = 0, item = -1;
			for(int i = 0; i < m; i++) {
				if(mat_m.get_bit_val(c, i) > 0) {
					item = i;
					vars_in_xor++;
					if(vars_in_xor > 1)
						continue;
				}
			}
			if(vars_in_xor == 1)
				try {
					p[item].removeValue((mat_m.get_bit_val(c, m)+1)%2, Cause.Null);
				} catch (ContradictionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					reset = true;
				}
		}
	}
	
	public static void printMatrix(final DenseMatrixGF2 m) {
        for (int i = 0; i < m.getRows(); i++) {
            for (int j = 0; j < m.getColumns(); j++) {
                System.out.print((m.get(i, j) ? 1 : 0) + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

}

