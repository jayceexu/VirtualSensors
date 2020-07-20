package bach.jianxu.watchsense;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;

public class Metaprogram {
    public static String TAG = "metaprogram";
    public String app_name;
    public String op;
    public String orientation;
    public ArrayList<String> sensors = new ArrayList<>();
    public HashMap<String, ArrayList<Double>> data = new HashMap<>();
    public HashMap<String, Integer> freqs = new HashMap<>();

    public ArrayList<String> mappingFrom = new ArrayList<>();
    public ArrayList<String> mappingTo = new ArrayList<>();

    void dump() {
        Log.d(TAG, "Dumping meta-program......");
        Log.d(TAG, "App name: " + app_name);
        Log.d(TAG, "op: " + op);
        Log.d(TAG, "orientation: " + orientation);
        Log.d(TAG, "Dumping meta-program......");
        for (HashMap.Entry<String,  ArrayList<Double>> entry : data.entrySet()) {
            String key = entry.getKey();
            ArrayList<Double> values = entry.getValue();
            Log.d(TAG, "the " + key + " sensor ");
            Log.d(TAG, "frequent sampling: " + freqs.get(key));
            Log.d(TAG, "X-calibrating: " + values.get(0));
            Log.d(TAG, "Y-calibrating: " + values.get(1));
            Log.d(TAG, "Z-calibrating: " + values.get(2));
        }
    }
}
