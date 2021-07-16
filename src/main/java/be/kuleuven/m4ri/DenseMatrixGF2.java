package be.kuleuven.m4ri;

/*
 * Copyright Vladimir Dzyuba
 * From Implementation of the Flexics pattern sampler.
 * Dzyuba, V., van Leeuwen, M., & De Raedt, L. (2016). Flexible constrained sampling with guarantees for pattern mining. arXiv:1610.09263.
*/

import malb.m4ri.SWIGTYPE_p_mzd_t;
import malb.m4ri.m4ri_jni;
import org.scijava.nativelib.NativeLoader;

import java.io.IOException;

public final class DenseMatrixGF2 {
	//*
    static {
    	System.loadLibrary("m4ri");
    	/*
        try {
            NativeLoader.loadLibrary("m4ri");
            //		"/home/hien/THESE/programs/eclipse/XOR_Test/resources/");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load M4RI from within the JAR", e);
        }
        //*/
    }
    //*/
    

    public static final int RADIX = m4ri_jni.getM4ri_radix();
    public static final int ECHELONIZE_RADIX = 128;

    public static DenseMatrixGF2 random(final int rows, final int columns) {
        final DenseMatrixGF2 m = new DenseMatrixGF2(rows, columns);
        m4ri_jni.mzd_randomize(m.matrix);
        return m;
    }

    private final int rows;
    private final int columns;
    private final SWIGTYPE_p_mzd_t matrix;

    private DenseMatrixGF2(final int rows,
                           final int columns,
                           final SWIGTYPE_p_mzd_t matrix) {
        this.rows = rows;
        this.columns = columns;
        this.matrix = matrix;
    }

    public DenseMatrixGF2(final int rows, final int columns) {
        this(rows, columns, m4ri_jni.mzd_init(rows, columns));
    }

    public DenseMatrixGF2 copy() {
        final SWIGTYPE_p_mzd_t newMatrix = m4ri_jni.mzd_init(rows, columns);
        m4ri_jni.mzd_copy(newMatrix, this.matrix);

        return new DenseMatrixGF2(this.rows, this.columns, newMatrix);
    }

    public DenseMatrixGF2 copyCorner(final int bottomMostRow, final int leftmostColumn) {
        final int newRows = bottomMostRow + 1;
        final int newColumns = columns - leftmostColumn;
        final SWIGTYPE_p_mzd_t newMatrix = m4ri_jni.mzd_submatrix(
                null, matrix,
                0, leftmostColumn,
                bottomMostRow + 1, columns
        );

        return new DenseMatrixGF2(newRows, newColumns, newMatrix);
    }

    public void free() {
        m4ri_jni.mzd_free(this.matrix);
    }

    /* @Override
    protected void finalize() throws Throwable {
        free();
        super.finalize();
    } */

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public boolean get(final int r, final int c) {
        return m4ri_jni.mzd_read_bit(this.matrix, r, c) == 1;
    }
    
    public int get_bit_val(final int r, final int c) {
        return m4ri_jni.mzd_read_bit(this.matrix, r, c);
    }

    public void set(final int r, final int c, final boolean v) {
        m4ri_jni.mzd_write_bit(this.matrix, r, c, v ? 1 : 0);
    }
    
    public void set(final int r, final int c, int v) {
        m4ri_jni.mzd_write_bit(this.matrix, r, c, v);
    }

    public void set(final int r, final int c) {
        set(r, c, true);
    }

    public void flip(final int r, final int c) {
        set(r, c, !get(r, c));
    }

    public void clearFirstBitsInRow(final int r, final int n) {
        if (n == 0)
            return;

        int bitsToClear = n;
        int c = 0;
        while (bitsToClear > 0) {
            m4ri_jni.mzd_clear_bits(matrix, r, c, Math.min(bitsToClear, RADIX));
            bitsToClear -= RADIX;
            c += RADIX;
        }
    }

    public void clearLeftColumns(final int n) {
        if (n == 0)
            return;

        for (int r = 0; r < rows; r++)
            clearFirstBitsInRow(r, n);
    }

    public void echelonize(final boolean reducedRow) {
        m4ri_jni.mzd_echelonize(this.matrix, reducedRow ? 1 : 0);
    }

    public void echelonizeUpperRightCorner(final int bottomRow, final int leftColumn, final boolean reducedRow) {
        assert(bottomRow < rows && leftColumn < columns - 1);
        final int effectiveLeftColumn = leftColumn - (leftColumn % ECHELONIZE_RADIX);
        final SWIGTYPE_p_mzd_t window = m4ri_jni.mzd_init_window(matrix,
                0, effectiveLeftColumn,
                bottomRow + 1, columns);
        m4ri_jni.mzd_echelonize(window, reducedRow ? 1 : 0);
        m4ri_jni.mzd_free_window(window);
    }

    public void swapRows(final int r1, final int r2) {
        m4ri_jni.mzd_row_swap(matrix, r1, r2);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("DenseMatrixGF2: ").append(rows).append('x').append(columns);
        for (int r = 0; r < rows; r++) {
            sb.append('\n');
            for (int c = 0; c < columns; c++) {
                sb.append(m4ri_jni.mzd_read_bit(this.matrix, r, c));
            }
        }
        return sb.toString();
    }
}
