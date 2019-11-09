package bach.jianxu.watchsense;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static bach.jianxu.watchsense.LinearRegression.TAG;

public class Matrix {


    public ArrayList<ArrayList<Float>> add(ArrayList<ArrayList<Float>> mat1, ArrayList<ArrayList<Float>> mat2) {
        ArrayList<ArrayList<Float>> added_matrix = new ArrayList<>();
        for ( int i = 0; i < mat1.size() ; i++)
        {
            ArrayList<Float> row_mat1 = new ArrayList<>(mat1.get(i));
            ArrayList<Float> row_mat2 = new ArrayList<>(mat2.get(i));
            ArrayList<Float> result = new ArrayList<>();
            if (row_mat1.size() != row_mat2.size())
                throw new IllegalArgumentException("Matix size mismatch. Matrix additions can be performed only on matrices of same dimensions.");
            for (int j = 0; j < row_mat1.size(); j++) {
                result.add(row_mat1.get(j) + row_mat2.get(j));
            }
            added_matrix.add(result);
        }
        return added_matrix;
    }


    public ArrayList<ArrayList<Float>> transpose(ArrayList<ArrayList<Float>> matrix) {
        ArrayList<ArrayList<Float>> inverse_matrix = new ArrayList<>();
        for (int i = 0; i < matrix.get(0).size(); i++) inverse_matrix.add(new ArrayList<Float>());
        for (int i = 0; i < matrix.size(); i++) {
            for ( int j = 0; j < matrix.get(i).size(); j++) {
                //inverse_matrix[j].add(matrix[i][j]);
                inverse_matrix.get(j).add(matrix.get(i).get(j));
            }
        }
        return inverse_matrix;
    }

    public ArrayList<ArrayList<Float>> mul(ArrayList<ArrayList<Float>> mat1, ArrayList<ArrayList<Float>> mat2) {
        ArrayList<ArrayList<Float>> mat_mul = new ArrayList<>();
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
            ArrayList<Float> row = new ArrayList<>();
            for (int j = 0; j < mat2.get(0).size(); j++) {
                row.add(0f);
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
                    float tmp = mat_mul.get(i).get(j);
                    mat_mul.get(i).set(j, tmp + mat1.get(i).get(k) * mat2.get(k).get(j));

                }
            }
        }
        return mat_mul;
    }

    public void scalar_multiply(float scalar_value, ArrayList<ArrayList<Float>> mat) {
        for (int row = 0; row < mat.size(); row++) {
            for(int column = 0; column < mat.get(row).size(); column++) {
                //mat[row][column] = mat[row][column] * scalar_value;
                mat.get(row).set(column, mat.get(row).get(column) * scalar_value);
            }
        }
    }

    public void  getCofactor(ArrayList<ArrayList<Float>> matrix, ArrayList<ArrayList<Float>> temp, int p, int q, int n) {
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

    void adjoint(ArrayList<ArrayList<Float>> matrix, ArrayList<ArrayList<Float>> adj) {
        int matrixSize = matrix.size();
        if (matrixSize == 1) {
            adj.get(0).set(0, 1f);
        }
        // temp is used to store cofactors of matrix[][]
        int sign = 1;

        int number_of_elements = matrixSize;
        float default_value = 1;
        int number_of_rows = 0;

        if (matrix.size() > 0) {
            number_of_rows = matrix.get(0).size();
        }

        // TODO: deep copy
        //ArrayList<Float> defaultValues(number_of_rows, default_value);
        ArrayList<Float> defaultValues = new ArrayList<>();
        for (int i = 0; i < number_of_rows; i++) defaultValues.add(default_value);
        //ArrayList<ArrayList<Float>> temp(number_of_elements, defaultValues);
        ArrayList<ArrayList<Float>> temp = new ArrayList<>();
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
                float tmp = (float)(sign)*(determinantOfMatrix(temp, matrixSize - 1));
                adj.get(j).set(i, tmp);
            }
        }

    }

    boolean slowInverse(ArrayList<ArrayList<Float>> matrix, ArrayList<ArrayList<Float>> inverse)
    {

        //ArrayList<ArrayList<Float>> inverse;
        int matrixSize = matrix.size();
        // Find determinant of matrix[][]
        float det = determinantOfMatrix(matrix, matrixSize);
        if (det == 0) return false;

        // Find adjoint
        int number_of_elements = matrix.size();
        float default_value = 1;
        int number_of_rows = 0;

        if (matrix.size() > 0) {
            number_of_rows = matrix.get(0).size();
        }

        // TODO: deep copy
        //ArrayList<Float> defaultValues(number_of_rows, default_value);
        ArrayList<Float> defaultValues = new ArrayList<>();
        for (int i = 0; i < number_of_rows; i++) defaultValues.add(default_value);
        //ArrayList<ArrayList<Float>> adj(number_of_elements, defaultValues);
        ArrayList<ArrayList<Float>> adj = new ArrayList<>();
        for (int i = 0; i < number_of_elements; i++) adj.add(defaultValues);

        adjoint(matrix, adj);

        // Find Inverse using formula "inverse(matrix) = adj(matrix)/det(matrix)"
        for (int i = 0; i<matrixSize; i++)
            for (int j = 0; j<matrixSize; j++)
                inverse.get(i).set(j,  adj.get(i).get(j) / det);

        return true;

    }

    public boolean inverse(ArrayList<ArrayList<Float>> matrix, ArrayList<ArrayList<Float>> inverse)
    {
        int matrixSize = matrix.size();
        // Find determinant of matrix[][]
        double det = determinantOfMatrix(matrix, matrixSize);
        if (det == 0)
        {
            return false;
        }
        Log.d(TAG, "inverse determinant " + det);

        ArrayList<ArrayList<Float>> augmented = new ArrayList<>();
        for (int i = 0; i < matrixSize; i++)
        {
            ArrayList<Float> row = new ArrayList<>();
            for (int j = 0; j < 2 * matrixSize; j++)
                row.add(0f);
            augmented.add(row);
        }
        for (int i = 0; i < matrixSize; i++)
        {
            for (int j = 0; j < matrix.get(i).size(); j++)
                augmented.get(i).set(j, matrix.get(i).get(j));

            //augmented[i][i + matrixSize] = 1.0;
            augmented.get(i).set(i + matrixSize, 1f);
        }

        for (int i = matrixSize - 1; i > 0; i--)
        {
            if (augmented.get(i-1).get(0) < augmented.get(i).get(0))
            {
                for (int j = 0; j < 2 * matrixSize; j++)
                {
                    float temp = augmented.get(i).get(j);
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
                    float temp = augmented.get(j).get(i) / augmented.get(i).get(i);
                    for (int k = 0; k < 2 * matrixSize; k++)
                    {
                        //augmented[j][k] -= augmented[i][k] * temp;
                        float tmp = augmented.get(j).get(k);
                        augmented.get(j).set(k, tmp- augmented.get(i).get(k) * temp);
                    }
                }
            }
        }

        for (int i = 0; i < matrixSize; i++) {
            float temp = augmented.get(i).get(i);
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

    public float determinantOfMatrix(ArrayList<ArrayList<Float>> matrix,  long n)  {
        float D;
        if (n == 1) {
            D = matrix.get(0).get(0);
            return D;
        }
        /**
         * Jian: should deep copy!
         */
        // Make L and U
        ArrayList<ArrayList<Float>> L = new ArrayList<>();
        for (int i = 0; i < matrix.size(); i++) {
            ArrayList<Float> row = new ArrayList<>();
            for (int j = 0; j < matrix.get(0).size(); j++)
                row.add(matrix.get(i).get(j));
            L.add(row);
        }
        ArrayList<ArrayList<Float>> U = new ArrayList<>();
        for (int i = 0; i < matrix.size(); i++) {
            ArrayList<Float> row = new ArrayList<>();
            for (int j = 0; j < matrix.get(0).size(); j++)
                row.add(matrix.get(i).get(j));
            U.add(row);
        }
        int i = 0, j = 0, k = 0;
        for (i = 0; i < n; i++) {
            for (j = 0; j < n; j++) {
                if (j < i)
                    L.get(j).set(i, 0f);
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
                    U.get(i).set(j, 0f);
                else if (j == i)
                    //U[i][j] = 1;
                    U.get(i).set(j, 1f);
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

        D = 1f;
        for (i = 0; i < n; i++) {
            D = D * U.get(i).get(i) * L.get(i).get(i);
        }
        return D;
    }

    public double slowDeterminantOfMatrix(ArrayList<ArrayList<Float>> matrix,  int n)
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
        ArrayList<Float> defaultValues = new ArrayList<>();
        for (int i = 0; i < number_of_rows; i++)  defaultValues.add((float)default_value);
        ArrayList<ArrayList<Float>> temp = new ArrayList<>();
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


