package bach.jianxu.watchsense;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import static java.util.Arrays.asList;

public class MainActivity extends Activity {

    private static String TAG = "WatchSense";
    public static final int PERMISSIONS_REQUEST_CODE = 1;
    private SensingService mSensingService = new SensingService();
    private Messenger mService;
    private boolean mBound;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button recordBtn = findViewById(R.id.record_btn);
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Gesture Saving");
                alertDialog.setMessage("Enter the name of the gesture, and then start your gesture:");
                final EditText input = new EditText(MainActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);
                /* This Save button does the following:
                 * 1. Ask users to input the name of the gesture;
                 * 2. Users should perform a screen gesture, after pressing 'Save' Button,
                 * */
                alertDialog.setPositiveButton("Save",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String gestureName = input.getText().toString();
                                recordGestures(gestureName != null ? gestureName : "default");
                            }
                        });

                alertDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                alertDialog.show();
                Toast.makeText(MainActivity.this, "Gesture saved." ,Toast.LENGTH_SHORT).show();
            }
        });

        Button clibrateBtn = findViewById(R.id.calibration_btn);
        clibrateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick() happens");
                ArrayList<ArrayList<Double>> X = new ArrayList<>();
                X.add(new ArrayList<>(asList(110.0, 40.0, 50.0)));
                X.add(new ArrayList<>(asList(120.0, 30.0, 40.0)));
                X.add(new ArrayList<>(asList(100.0, 20.0, 60.0)));
                X.add(new ArrayList<>(asList(90.0, 0.0, 30.0)));
                X.add(new ArrayList<>(asList(80.0, 10.0, 20.0)));

                ArrayList<Double> Y = new ArrayList<>(asList(100.0, 90.0, 80.0, 70.0, 60.0));
                LinearRegression mlr = new LinearRegression(X, Y);
                mlr.fit();
                ArrayList<Double> parameter = new ArrayList<>(asList(110.0, 40.0, 50.0));
                Log.d(TAG, "result " + mlr.predict(parameter));

                Message msg = Message.obtain(null, 1, 0, 0);
                try {
                    mService.send(msg);
                } catch (Exception e) {e.printStackTrace();}
            }
        });
        //Intent aint = new Intent(this, SensingService.class);
        bindService(new Intent(this, SensingService.class), mConnection,
                Context.BIND_AUTO_CREATE);
        //startService(aint);
        checkPermissions();
    }

    // TODO: should support more shaking gestures, more than just screen touching

    /**
     * This function just support recording screen touching/swiping gestures
     * @param gestureName: the name of the recorded screen gesture, will be used for metaprogram
     */
    private void recordGestures(String gestureName) {
        if (Shell.isSuAvailable()) {
            String fname = "/sdcard/temp/recorded_gesture_"
                    + gestureName.trim().replaceAll(" ", "_").toLowerCase() + ".txt";
            String command = "/data/local/tmp/getevent -t /dev/input/event1 > " + fname;
            Log.d(TAG, "Command is: " + command);
            Shell.runCommand(command);
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };

    private void checkPermissions() {
        boolean writeExternalStoragePermissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;

        if (!writeExternalStoragePermissionGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_CODE);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
}
