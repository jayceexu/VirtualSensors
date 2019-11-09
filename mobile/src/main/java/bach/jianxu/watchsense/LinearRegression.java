package bach.jianxu.watchsense;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class LinearRegression {

    public final static String TAG = "LinearRegression";
    // Independent variable X
    ArrayList<ArrayList<Float>> X;

    ArrayList<Float> y;

    ArrayList<Float> bias = new ArrayList<>();


    LinearRegression(String model_name)
    {

    }
    LinearRegression(ArrayList<ArrayList<Float>> XX, ArrayList<Float> yy) {
        X = new ArrayList<>(XX);
        y = new ArrayList<>(yy);
    }
    void fit() {
        // Adding bias to X
        for (int i = 0; i < X.size(); i++) {
            ArrayList<Float> row = new ArrayList<>();
            row.add(1f);
            for (float j : X.get(i)) {
                row.add(j);
            }
            X.set(i, row);
        }
        // X'
        ArrayList<ArrayList<Float>> X_transpose = new ArrayList<>();

        // X'X
        ArrayList<ArrayList<Float>> X_transpose_X = new ArrayList<>();

        Matrix mat = new Matrix();
        X_transpose = mat.transpose(X);
        X_transpose_X = mat.mul(X_transpose, X);

        ArrayList<Float> rows = new ArrayList<>();
        for (int i = 0; i < X_transpose_X.get(0).size(); i++) rows.add(1f);

        ArrayList<ArrayList<Float>> inverse_of_X_transpose_X = new ArrayList<>();
        for (int i = 0; i < X_transpose_X.size(); i++) inverse_of_X_transpose_X.add(new ArrayList<>(rows));
        mat.inverse(X_transpose_X, inverse_of_X_transpose_X);


        ArrayList<ArrayList<Float>> y_reshaped = new ArrayList<>();
        for (Float i : y) {
            ArrayList<Float> row = new ArrayList<>();
            row.add(i);
            y_reshaped.add(row);
        }
        ArrayList<ArrayList<Float>> X_transpose_y = new ArrayList<>(mat.mul(X_transpose, y_reshaped));
        ArrayList<ArrayList<Float>> b = new ArrayList<>(mat.mul(inverse_of_X_transpose_X, X_transpose_y));
        for (ArrayList<Float> i : b) {
            bias.add(i.get(0));
        }
        Log.d(TAG, "Found");
    }

    float predict(ArrayList<Float> test) {
        float prediction = 0f;
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

    ArrayList<Float> get_bias() {
        return bias;
    }

    void print(String message) {
        Log.d(TAG, message);
    }

}