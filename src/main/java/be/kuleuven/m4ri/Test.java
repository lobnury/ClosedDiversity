package be.kuleuven.m4ri;

/*
 * Copyright Vladimir Dzyuba
 * From Implementation of the Flexics pattern sampler.
 * Dzyuba, V., van Leeuwen, M., & De Raedt, L. (2016). Flexible constrained sampling with guarantees for pattern mining. arXiv:1610.09263.
*/

public class Test {
    public static void main(String[] args) {
        final int rows = 5;
        final int columns = 300;

        final int bottomRow = 3;
        final int leftColumn = 64*4;

        final DenseMatrixGF2 m = DenseMatrixGF2.random(rows, columns);
        System.out.println("Original matrix");
        printMatrix(m);
        System.out.println("Original matrix, window");
        printMatrix(m, bottomRow, leftColumn);

        final DenseMatrixGF2 c = m.copyCorner(bottomRow, leftColumn);
        System.out.println("Window copy");
        printMatrix(c);
        c.echelonize(true);
        System.out.println("Echelonised window copy");
        printMatrix(c);


        m.echelonizeUpperRightCorner(bottomRow, leftColumn, true);
        System.out.println("Original matrix with window echelonised");
        printMatrix(m);
        System.out.println("Original matrix with window echelonised, only window");
        printMatrix(m, bottomRow, leftColumn);

        for (int i = 0; i <= bottomRow; i++) {
            for (int j = 0; j < c.getColumns(); j++) {
                System.out.print(c.get(i, j) != m.get(i, leftColumn + j) ? '1' : '_');
            }
            System.out.println();
        }

        c.free();
        m.free();
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

    public static void printMatrix(final DenseMatrixGF2 m, final int toRow, final int fromColumn) {
        for (int i = 0; i <= toRow; i++) {
            for (int j = fromColumn; j < m.getColumns(); j++) {
                System.out.print((m.get(i, j) ? 1 : 0) + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
