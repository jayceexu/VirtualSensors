package bach.jianxu.watchsense;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;

public class Metaprogram {

    // TODO: reshaping all of this
    public static String TAG = "metaprogram";
    public String app_name;

    public String sensor_type_from = "";
    public String sensor_type_to = "";

    public String semantics_from = "";
    public String semantics_to = "";

    public String freq;
    public Boolean isOverride = false;

    void dump() {
        Log.d(TAG, "Dumping meta-program......");
        Log.d(TAG, "App name: " + app_name);
        Log.d(TAG, "Sensor mapping from : " + sensor_type_from);
        Log.d(TAG, "Sensor mapping to : " + sensor_type_to);
        Log.d(TAG, "frequent sampling: " + freq);
        Log.d(TAG, "isOverride: " + isOverride);
        Log.d(TAG, "Semantics from: " + semantics_from);
        Log.d(TAG, "Semantics to: " + semantics_to);
    }
}
