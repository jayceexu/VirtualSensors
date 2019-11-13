package bach.jianxu.watchsense;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class LinearRegression {

    public final static String TAG = "LinearRegression";
    // Independent variable X
    ArrayList<ArrayList<Double>> X;

    ArrayList<Double> y;

    ArrayList<Double> bias = new ArrayList<>();


    LinearRegression(String model_name)
    {

    }
    LinearRegression(ArrayList<ArrayList<Double>> XX, ArrayList<Double> yy) {
        X = new ArrayList<>(XX);
        y = new ArrayList<>(yy);
    }
    void fit() {
        // Adding bias to X
        for (int i = 0; i < X.size(); i++) {
            ArrayList<Double> row = new ArrayList<>();
            row.add(1.0);
            for (Double j : X.get(i)) {
                row.add(j);
            }
            X.set(i, row);
        }
        // X'
        ArrayList<ArrayList<Double>> X_transpose = new ArrayList<>();

        // X'X
        ArrayList<ArrayList<Double>> X_transpose_X = new ArrayList<>();

        Matrix mat = new Matrix();
        X_transpose = mat.transpose(X);
        X_transpose_X = mat.mul(X_transpose, X);

        ArrayList<Double> rows = new ArrayList<>();
        for (int i = 0; i < X_transpose_X.get(0).size(); i++) rows.add(1.0);

        ArrayList<ArrayList<Double>> inverse_of_X_transpose_X = new ArrayList<>();
        for (int i = 0; i < X_transpose_X.size(); i++) inverse_of_X_transpose_X.add(new ArrayList<>(rows));
        mat.inverse(X_transpose_X, inverse_of_X_transpose_X);


        ArrayList<ArrayList<Double>> y_reshaped = new ArrayList<>();
        for (Double i : y) {
            ArrayList<Double> row = new ArrayList<>();
            row.add(i);
            y_reshaped.add(row);
        }
        ArrayList<ArrayList<Double>> X_transpose_y = new ArrayList<>(mat.mul(X_transpose, y_reshaped));
        ArrayList<ArrayList<Double>> b = new ArrayList<>(mat.mul(inverse_of_X_transpose_X, X_transpose_y));
        for (ArrayList<Double> i : b) {
            bias.add(i.get(0));
        }
        Log.d(TAG, "Found");
    }

    Double predict(ArrayList<Double> test) {
        double prediction = 0.0;
        prediction += bias.get(0);
        for (int i = 0; i < test.size(); i++) {
            double value = bias.get(i+1) * test.get(i);
            Log.d(TAG, "predictvalue " + value);
            prediction += value;
        }
        return prediction;
    }

    void save_model(String model_name){


    }

    ArrayList<Double> get_bias() {
        return bias;
    }

    void print(String message) {
        Log.d(TAG, message);
    }

}