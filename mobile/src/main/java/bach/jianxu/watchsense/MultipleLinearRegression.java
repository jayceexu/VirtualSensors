package bach.jianxu.watchsense;

/******************************************************************************
 *  Compilation:  javac -classpath jama.jar:. MultipleLinearRegression.java
 *  Execution:    java  -classpath jama.jar:. MultipleLinearRegression
 *  Dependencies: jama.jar
 *  
 *  Compute least squares solution to X beta = y using Jama library.
 *  Assumes X has full column rank.
 *  
 *       http://math.nist.gov/javanumerics/jama/
 *       http://math.nist.gov/javanumerics/jama/Jama-1.0.1.jar
 *
 ******************************************************************************/

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;

import Jama.Matrix;
import Jama.QRDecomposition;


/**
 *  The {@code MultipleLinearRegression} class performs a multiple linear regression
 *  on an set of <em>N</em> data points using the model
 *  <em>y</em> = &beta;<sub>0</sub> + &beta;<sub>1</sub> <em>x</em><sub>1</sub> + ... + 
    &beta;<sub><em>p</em></sub> <em>x<sub>p</sub></em>,
 *  where <em>y</em> is the response (or dependent) variable,
 *  and <em>x</em><sub>1</sub>, <em>x</em><sub>2</sub>, ..., <em>x<sub>p</sub></em>
 *  are the <em>p</em> predictor (or independent) variables.
 *  The parameters &beta;<sub><em>i</em></sub> are chosen to minimize
 *  the sum of squared residuals of the multiple linear regression model.
 *  It also computes the coefficient of determination <em>R</em><sup>2</sup>.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
public class MultipleLinearRegression {
    private final Matrix beta;  // regression coefficients
    private double sse;         // sum of squared
    private double sst;         // sum of squared
    private MultipleLinearRegression m_x_regressor;
    private MultipleLinearRegression m_y_regressor;
    private MultipleLinearRegression m_z_regressor;
    private String TAG = "MultipleLinearRegression";

    public MultipleLinearRegression() {beta = null;}
   /**
     * Performs a linear regression on the data points {@code (y[i], x[i][j])}.
     * @param  x the values of the predictor variables
     * @param  y the corresponding values of the response variable
     */
    public MultipleLinearRegression(double[][] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("matrix dimensions don't agree");
        }

        // number of observations
        int n = y.length;

        Matrix matrixX = new Matrix(x);

        // create matrix from vector
        Matrix matrixY = new Matrix(y, n);

        // find least squares solution
        QRDecomposition qr = new QRDecomposition(matrixX);
        beta = qr.solve(matrixY);


        // mean of y[] values
        double sum = 0.0;
        for (int i = 0; i < n; i++)
            sum += y[i];
        double mean = sum / n;

        // total variation to be accounted for
        for (int i = 0; i < n; i++) {
            double dev = y[i] - mean;
            sst += dev*dev;
        }

        // variation not accounted for
        Matrix residuals = matrixX.times(beta).minus(matrixY);
        sse = residuals.norm2() * residuals.norm2();

    }

   /**
     * Returns the least squares estimate of &beta;<sub><em>j</em></sub>.
     *
     * @param  j the index
     * @return the estimate of &beta;<sub><em>j</em></sub>
     */
    public double beta(int j) {
        return beta.get(j, 0);
    }

   /**
     * Returns the coefficient of determination <em>R</em><sup>2</sup>.
     *
     * @return the coefficient of determination <em>R</em><sup>2</sup>,
     *         which is a real number between 0 and 1
     */
    public double R2() {
        return 1.0 - sse/sst;
    }

   /**
     * Unit tests the {@code MultipleLinearRegression} data type.
     * calibrating from dev_* to ph_*
     *
     */
    public void calibrate(ArrayList<ArrayList<Double>> srcMatrix,
                          ArrayList<ArrayList<Double>> dstMatrix) {
        if (srcMatrix.size() != dstMatrix.size()) {
            Log.e(TAG, "calibrate Failed: tuple size is not equivalent");
            return;
        }
	    int num_data_points = srcMatrix.size();
	    double[][] arr = new double[num_data_points][4];
        double [] x = new double[num_data_points];
        double [] y = new double[num_data_points];
        double [] z = new double[num_data_points];
	    for (int i = 0; i < num_data_points; i++) {
		    arr[i][0] = 1;
		    arr[i][1] = srcMatrix.get(i).get(0);
		    arr[i][2] = srcMatrix.get(i).get(1);
		    arr[i][3] = srcMatrix.get(i).get(2);
		    x[i] = dstMatrix.get(i).get(0);
		    y[i] = dstMatrix.get(i).get(1);
		    z[i] = dstMatrix.get(i).get(2);
	    }
        m_x_regressor = new MultipleLinearRegression(arr, x);
	    m_y_regressor = new MultipleLinearRegression(arr, y);
	    m_z_regressor = new MultipleLinearRegression(arr, z);
        Log.i(TAG, "Now finished calibrating the " + num_data_points + " sets of data");
    }

    public void test_regression(double dev_x[], double dev_y[], double dev_z[]) {
	    double[] ph_x = new double[dev_x.length];
	    for (int i = 0; i < dev_x.length; i++) {
	    	ph_x[i] = m_x_regressor.beta(0) + m_x_regressor.beta(1) * dev_x[i]
			+ m_x_regressor.beta(2) * dev_x[i] + m_x_regressor.beta(3) * dev_x[i];
	    }
	    System.out.println("Output = " + ph_x);
    }

    /**
     * convert matrix from watch to phone
     * @param matrix: it has matrix.size() tuples, each tuple has 3 values: x, y, z
     *
     */
    public ArrayList<ArrayList<Double>> convert(ArrayList<ArrayList<Double>> matrix) {
        int n = matrix.size();
        ArrayList<ArrayList<Double>> res = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ArrayList<Double> tuple = new ArrayList<>(3);
            double v =  m_x_regressor.beta(0) + m_x_regressor.beta(1) * matrix.get(i).get(0)
                                            + m_x_regressor.beta(2) * matrix.get(i).get(0)
                                            + m_x_regressor.beta(3) * matrix.get(i).get(0);
            tuple.add(v);

            v =  m_x_regressor.beta(0) + m_x_regressor.beta(1) * matrix.get(i).get(1)
                                        + m_x_regressor.beta(2) * matrix.get(i).get(1)
                                        + m_x_regressor.beta(3) * matrix.get(i).get(1);
            tuple.add(v);

            v =  m_x_regressor.beta(0) + m_x_regressor.beta(1) * matrix.get(i).get(2)
                                        + m_x_regressor.beta(2) * matrix.get(i).get(2)
                                        + m_x_regressor.beta(3) * matrix.get(i).get(2);
            tuple.add(v);

            res.add(tuple);
        }
        return res;

    }
}

