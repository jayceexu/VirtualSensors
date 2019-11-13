package bach.jianxu.watchsense;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static bach.jianxu.watchsense.LinearRegression.TAG;

public class Matrix {


    public ArrayList<ArrayList<Double>> add(ArrayList<ArrayList<Double>> mat1, ArrayList<ArrayList<Double>> mat2) {
        ArrayList<ArrayList<Double>> added_matrix = new ArrayList<>();
        for ( int i = 0; i < mat1.size() ; i++)
        {
            ArrayList<Double> row_mat1 = new ArrayList<>(mat1.get(i));
            ArrayList<Double> row_mat2 = new ArrayList<>(mat2.get(i));
            ArrayList<Double> result = new ArrayList<>();
            if (row_mat1.size() != row_mat2.size())
                throw new IllegalArgumentException("Matix size mismatch. Matrix additions can be performed only on matrices of same dimensions.");
            for (int j = 0; j < row_mat1.size(); j++) {
                result.add(row_mat1.get(j) + row_mat2.get(j));
            }
            added_matrix.add(result);
        }
        return added_matrix;
    }


    public ArrayList<ArrayList<Double>> transpose(ArrayList<ArrayList<Double>> matrix) {
        ArrayList<ArrayList<Double>> inverse_matrix = new ArrayList<>();
        for (int i = 0; i < matrix.get(0).size(); i++) inverse_matrix.add(new ArrayList<Double>());
        for (int i = 0; i < matrix.size(); i++) {
            for ( int j = 0; j < matrix.get(i).size(); j++) {
                //inverse_matrix[j].add(matrix[i][j]);
                inverse_matrix.get(j).add(matrix.get(i).get(j));
            }
        }
        return inverse_matrix;
    }

    public ArrayList<ArrayList<Double>> mul(ArrayList<ArrayList<Double>> mat1, ArrayList<ArrayList<Double>> mat2) {
        ArrayList<ArrayList<Double>> mat_mul = new ArrayList<>();
        int r1 = mat1.size();
        if (r1 == 0) throw new IllegalArgumentException("Matrix - 1 Trying to multiply empty matrices!");
        int c1 = mat1.get(0).size();
        int r2 = mat2.size();
        if (r2 == 0) throw new IllegalArgumentException("Matrix - 2 Trying to multiply empty matrices!");
        int c2 = mat2.get(0).size();
        // Initialize with empty matrix
        //std::cout << "C1 " << c1 << " r2 " << r2 << "\n";
        for (int i = 0; i < mat1.size(); i++)
        {
            ArrayList<Double> row = new ArrayList<>();
            for (int j = 0; j < mat2.get(0).size(); j++) {
                row.add(0.0);
            }
            mat_mul.add(row);
        }
        for (int i = 0; i < r1; i++)
        {
            for (int j = 0; j < c2; j++)
            {
                for (int k = 0; k < r2; k++)
                {
                    //mat_mul[i][j] += mat1[i][k] * mat2[k][j];
                    Double tmp = mat_mul.get(i).get(j);
                    mat_mul.get(i).set(j, tmp + mat1.get(i).get(k) * mat2.get(k).get(j));
                }
            }
        }
        return mat_mul;
    }

    public void scalar_multiply(Double scalar_value, ArrayList<ArrayList<Double>> mat) {
        for (int row = 0; row < mat.size(); row++) {
            for(int column = 0; column < mat.get(row).size(); column++) {
                //mat[row][column] = mat[row][column] * scalar_value;
                mat.get(row).set(column, mat.get(row).get(column) * scalar_value);
            }
        }
    }

    public void  getCofactor(ArrayList<ArrayList<Double>> matrix, ArrayList<ArrayList<Double>> temp, int p, int q, int n) {
        int i = 0, j = 0;
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                if (row != p && col != q) {
                    //temp[i][j++] = matrix[row][col];
                    temp.get(i).set(j++, matrix.get(row).get(col));
                    if (j == n - 1) {
                        j = 0;
                        i++;
                    }
                }
            }
        }
    }

    void adjoint(ArrayList<ArrayList<Double>> matrix, ArrayList<ArrayList<Double>> adj) {
        int matrixSize = matrix.size();
        if (matrixSize == 1) {
            adj.get(0).set(0, 1.0);
        }
        // temp is used to store cofactors of matrix[][]
        int sign = 1;

        int number_of_elements = matrixSize;
        double default_value = 1;
        int number_of_rows = 0;

        if (matrix.size() > 0) {
            number_of_rows = matrix.get(0).size();
        }

        // TODO: deep copy
        //ArrayList<Double> defaultValues(number_of_rows, default_value);
        ArrayList<Double> defaultValues = new ArrayList<>();
        for (int i = 0; i < number_of_rows; i++) defaultValues.add(default_value);
        //ArrayList<ArrayList<Double>> temp(number_of_elements, defaultValues);
        ArrayList<ArrayList<Double>> temp = new ArrayList<>();
        for (int i = 0; i < number_of_elements; i++) temp.add(defaultValues);

        for (int i = 0; i<matrixSize; i++)
        {
            for (int j = 0; j<matrixSize; j++)
            {
                // Get cofactor of matrix[i][j]
                getCofactor(matrix, temp, i, j, matrixSize);

                // sign of adj[j][i] positive if sum of row
                // and column indexes is even.
                sign = ((i + j) % 2 == 0) ? 1 : -1;

                // Interchanging rows and columns to get the
                // transpose of the cofactor matrix
                //adj[j][i] = (sign)*(determinantOfMatrix(temp, matrixSize - 1));
                Double tmp = (double)(sign)*(determinantOfMatrix(temp, matrixSize - 1));
                adj.get(j).set(i, tmp);
            }
        }

    }

    boolean slowInverse(ArrayList<ArrayList<Double>> matrix, ArrayList<ArrayList<Double>> inverse)
    {

        //ArrayList<ArrayList<Double>> inverse;
        int matrixSize = matrix.size();
        // Find determinant of matrix[][]
        double det = determinantOfMatrix(matrix, matrixSize);
        if (det == 0) return false;

        // Find adjoint
        int number_of_elements = matrix.size();
        double default_value = 1.0;
        int number_of_rows = 0;

        if (matrix.size() > 0) {
            number_of_rows = matrix.get(0).size();
        }

        // TODO: deep copy
        //ArrayList<Double> defaultValues(number_of_rows, default_value);
        ArrayList<Double> defaultValues = new ArrayList<>();
        for (int i = 0; i < number_of_rows; i++) defaultValues.add(default_value);
        //ArrayList<ArrayList<Double>> adj(number_of_elements, defaultValues);
        ArrayList<ArrayList<Double>> adj = new ArrayList<>();
        for (int i = 0; i < number_of_elements; i++) adj.add(defaultValues);

        adjoint(matrix, adj);

        // Find Inverse using formula "inverse(matrix) = adj(matrix)/det(matrix)"
        for (int i = 0; i<matrixSize; i++)
            for (int j = 0; j<matrixSize; j++)
                inverse.get(i).set(j,  adj.get(i).get(j) / det);

        return true;

    }

    public boolean inverse(ArrayList<ArrayList<Double>> matrix, ArrayList<ArrayList<Double>> inverse)
    {
        int matrixSize = matrix.size();
        // Find determinant of matrix[][]
        double det = determinantOfMatrix(matrix, matrixSize);
        if (det == 0)
        {
            return false;
        }
        Log.d(TAG, "inverse determinant " + det);

        ArrayList<ArrayList<Double>> augmented = new ArrayList<>();
        for (int i = 0; i < matrixSize; i++)
        {
            ArrayList<Double> row = new ArrayList<>();
            for (int j = 0; j < 2 * matrixSize; j++)
                row.add(0.0);
            augmented.add(row);
        }
        for (int i = 0; i < matrixSize; i++)
        {
            for (int j = 0; j < matrix.get(i).size(); j++)
                augmented.get(i).set(j, matrix.get(i).get(j));

            //augmented[i][i + matrixSize] = 1.0;
            augmented.get(i).set(i + matrixSize, 1.0);
        }

        for (int i = matrixSize - 1; i > 0; i--)
        {
            if (augmented.get(i-1).get(0) < augmented.get(i).get(0))
            {
                for (int j = 0; j < 2 * matrixSize; j++)
                {
                    Double temp = augmented.get(i).get(j);
                    //augmented[i][j] = augmented[i - 1][j];
                    augmented.get(i).set(j, augmented.get(i-1).get(j));
                    //augmented[i - 1][j] = temp;
                    augmented.get(i-1).set(j, temp);
                }
            }
        }

        for (int i = 0; i < matrixSize; i++)
        {
            for (int j = 0; j < matrixSize; j++)
            {
                if (j != i)
                {
                    Double temp = augmented.get(j).get(i) / augmented.get(i).get(i);
                    for (int k = 0; k < 2 * matrixSize; k++)
                    {
                        //augmented[j][k] -= augmented[i][k] * temp;
                        Double tmp = augmented.get(j).get(k);
                        augmented.get(j).set(k, tmp- augmented.get(i).get(k) * temp);
                    }
                }
            }
        }

        for (int i = 0; i < matrixSize; i++) {
            Double temp = augmented.get(i).get(i);
            for (int j = 0; j < 2 * matrixSize; j++) {
                augmented.get(i).set(j, augmented.get(i).get(j) / temp);
            }
        }

        for (int i = 0; i < matrixSize; i++)
        {
            for (int j = 0; j < matrixSize; j++)
            {
                inverse.get(i).set(j, augmented.get(i).get(matrixSize + j));
            }
        }

        return true;

    }

    public Double determinantOfMatrix(ArrayList<ArrayList<Double>> matrix,  long n)  {
        Double D;
        if (n == 1) {
            D = matrix.get(0).get(0);
            return D;
        }
        /**
         * Jian: should deep copy!
         */
        // Make L and U
        ArrayList<ArrayList<Double>> L = new ArrayList<>();
        for (int i = 0; i < matrix.size(); i++) {
            ArrayList<Double> row = new ArrayList<>();
            for (int j = 0; j < matrix.get(0).size(); j++)
                row.add(matrix.get(i).get(j));
            L.add(row);
        }
        ArrayList<ArrayList<Double>> U = new ArrayList<>();
        for (int i = 0; i < matrix.size(); i++) {
            ArrayList<Double> row = new ArrayList<>();
            for (int j = 0; j < matrix.get(0).size(); j++)
                row.add(matrix.get(i).get(j));
            U.add(row);
        }
        int i = 0, j = 0, k = 0;
        for (i = 0; i < n; i++) {
            for (j = 0; j < n; j++) {
                if (j < i)
                    L.get(j).set(i, 0.0);
                else {
                    //L[j][i] = matrix.get(j).get(i);
                    L.get(j).set(i, matrix.get(j).get(i));
                    for (k = 0; k < i; k++) {
                        //L[j][i] = L[j][i] - L[j][k] * U[k][i];
                        L.get(j).set(i, L.get(j).get(i) - L.get(j).get(k) * U.get(k).get(i));
                    }
                }
            }
            for (j = 0; j < n; j++) {
                if (j < i)
                    U.get(i).set(j, 0.0);
                else if (j == i)
                    //U[i][j] = 1;
                    U.get(i).set(j, 1.0);
                else {
                    //U[i][j] = matrix[i][j] / L[i][i];
                    U.get(i).set(j, matrix.get(i).get(j) / L.get(i).get(i));
                    for (k = 0; k < i; k++) {
                        //U[i][j] = U[i][j] - ((L[i][k] * U[k][j]) / L[i][i]);
                        U.get(i).set(j, U.get(i).get(j) - ((L.get(i).get(k) * U.get(k).get(j)) / L.get(i).get(i)));
                    }
                }
            }
        }

        D = 1.0;
        for (i = 0; i < n; i++) {
            D = D * U.get(i).get(i) * L.get(i).get(i);
        }
        return D;
    }

    public double slowDeterminantOfMatrix(ArrayList<ArrayList<Double>> matrix,  int n)
    {
        double D = 0.0;
        if (n == 1) {
            D = matrix.get(0).get(0);
            return D;
        }
        int sign = 1; // To store sign multiplier
        int number_of_elements = matrix.size();
        int default_value = 1;
        int number_of_rows = 0;

        if (matrix.size() > 0) {
            number_of_rows = matrix.get(0).size();
        }

        // TODO: deep copy
        ArrayList<Double> defaultValues = new ArrayList<>();
        for (int i = 0; i < number_of_rows; i++)  defaultValues.add((double)default_value);
        ArrayList<ArrayList<Double>> temp = new ArrayList<>();
        for (int i = 0; i < number_of_elements; i++) temp.add(defaultValues);
        for (int f = 0; f < n; f++)
        {
            getCofactor(matrix, temp, 0, f, n);

            D += sign * matrix.get(0).get(f) * slowDeterminantOfMatrix(temp, n - 1);
            sign = -sign;
        }
        return D;
    }
};


