package bach.jianxu.watchsense;

import android.util.Log;
import java.util.ArrayList;

public class Metaprogram {
    public static String TAG = "metaprogram";
    public String app_name;
    public String op;
    public ArrayList<String> sensors = new ArrayList<>();
    public ArrayList<Integer> freq = new ArrayList<>();
    public ArrayList<Integer> xcal = new ArrayList<>();
    public ArrayList<Integer> ycal = new ArrayList<>();
    public ArrayList<Integer> zcal = new ArrayList<>();

    void dump() {
        Log.d(TAG, "Dumping meta-program......");
        Log.d(TAG, "App name: " + app_name);
        Log.d(TAG, "op: " + op);
        for (int i = 0; i < sensors.size(); ++i) {
            Log.d(TAG, "the " + i + "-th sensor " + sensors.get(i));
            Log.d(TAG, "frequent sampling: " + freq.get(i));
            Log.d(TAG, "X-calibrating: " + xcal.get(i));
            Log.d(TAG, "Y-calibrating: " + ycal.get(i));
            Log.d(TAG, "Z-calibrating: " + zcal.get(i));
        }
    }
}
