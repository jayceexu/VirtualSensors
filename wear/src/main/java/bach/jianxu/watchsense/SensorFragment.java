package bach.jianxu.watchsense;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.WorkerThread;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import static android.content.Context.VIBRATOR_SERVICE;

public class SensorFragment extends Fragment implements
        SensorEventListener,
        DataApi.DataListener,
        MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks {

    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final float ROTATION_THRESHOLD = 2.0f;
    private static final int ROTATION_WAIT_TIME_MS = 100;

    private static float mProximity = 100;  //initial value for proximity sensor, for dedup

    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "WatchSense";
    private static final String WEAR_MESSAGE_PATH = "/message";

    private View mView;
    private TextView mAccelero;
    private TextView mGyroscope;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Sensor mSensor2;
    private Sensor mSensorAll;

    private int mSensorType;
    private long mShakeTime = 0;
    private long mRotationTime = 0;
    private LinkedBlockingQueue<String> mQueue = new LinkedBlockingQueue<>(100);

    private static Activity mAct;
    private boolean mEmpty;
    private String mMessage = "";
    private static long statID = 1; // for statistics purposes,

    private boolean sampleGyro = false;
    private boolean sampleAccel = false;
    private boolean sampleAmbient = false;

    BlockingQueue mSharedQueue = new LinkedBlockingQueue<String>();
    private Thread mConsumer = new Thread(new Consumer(mSharedQueue));

    private Vibrator mVibrator;


    static private boolean FLAG_GAME = true;

    public static SensorFragment newInstance(int sensorType, Activity ap) {
        SensorFragment f = new SensorFragment();

        // Supply sensorType as an argument
        Bundle args = new Bundle();
        args.putInt("sensorType", sensorType);
        f.setArguments(args);
        mAct = ap;
        return f;
    }


    // Should use Thread instead of AsyncTask as this is a long turn workload
    private Thread mThread = new Thread(new Runnable() {
        private Socket mSocket;
        private PrintWriter mOutput;
        private OutputStream mOut;
        private ConcurrentLinkedQueue<String> mLocal = new ConcurrentLinkedQueue<>();
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(2000);
                    Log.i(TAG, "heart beating....");
                    String msg = "heart beat";
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                    for (Node node: nodes.getNodes()) {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                mGoogleApiClient, node.getId(), WEAR_MESSAGE_PATH, msg.getBytes() ).await();
                        Log.d("XUJAY_TCP", "sending msg in batch from another thread.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(mAct)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();

        Bundle args = getArguments();
        if(args != null) {
            mSensorType = args.getInt("sensorType");
        }
        mEmpty = true;
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(mSensorType);
        mSensorAll = mSensorManager.getDefaultSensor(Sensor.TYPE_ALL);
        mSensor2 = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        mVibrator = (Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE);

        mThread.start();
        mConsumer.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.sensor, container, false);

        mAccelero = mView.findViewById(R.id.txt_accelero);
        mGyroscope =  mView.findViewById(R.id.txt_gyroscope);

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorAll, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensor2, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    float[] inR = new float[16];
    float[] I = new float[16];
    float[] gravity = new float[3];
    float[] geomag = new float[3];
    float[] orientVals = new float[3];

    double azimuth = 0;
    double pitch = 0;
    double roll = 0;
    @Override
    public void onSensorChanged(SensorEvent event) {
        float ax = event.values[0];
        float ay = event.values[1];
        float az = event.values[2];
        String msg = "";
        statID++;
        /**
         * The schema of msg is: [sensor_type]:[uniq_statID],[data1],[data2],...,@
        */
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            /**
             * Light sensor needs to transfer immediately, without batching
             * This design is to deduplicate data, otherwise the proximity data will be noisy
             *  value:5 is enough for proximity distance
             * */
            ax = ax>= 5 ? 5: 0;
            if (ax == mProximity) return;
            mProximity = ax;
            msg += "proximity:" + + statID + "," + String.valueOf(ax) + "," + String.valueOf(ay) + "," + String.valueOf(az) + ",@";
            Log.i("XUJAY_MM", "proximity:" + + statID + "," + String.valueOf(ax) + "," + String.valueOf(ay) + "," + String.valueOf(az) + ",@");

        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomag = event.values.clone();

        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone();

            float af = (float) Math.sqrt(Math.pow(ax, 2)+ Math.pow(ay, 2)+ Math.pow(az, 2));
            /*mAccelero.setText("\nAccelerometer :"+"\n"+
                    "\u00E2x: "+ String.valueOf(ax)+"\n"+
                    "\u00E2y: "+ String.valueOf(ay)+"\n"+
                    "\u00E2z: "+ String.valueOf(az)+"\n"+
                    "\u00E2Net: "+ String.valueOf(af)
            );*/
            msg += "accel:" + statID + "," + String.valueOf(ax) + "," + String.valueOf(ay) + "," + String.valueOf(az) + ",@";
            // Warning: this log can slow down performance severely
            //Log.i("XUJAY_MM", "accel:" + statID + "," + String.valueOf(ax) + "," + String.valueOf(ay) + "," + String.valueOf(az) + ",@");

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && sampleGyro) {
            float gf = (float) Math.sqrt(Math.pow(ax, 2)+ Math.pow(ay, 2)+ Math.pow(az, 2));
            /*mGyroscope.setText("\nGyroscope :"+"\n"+
                    "\u03A9x: "+ String.valueOf(ax)+"\n"+
                    "\u03A9y: "+ String.valueOf(ay)+"\n"+
                     "\u03A9z: "+ String.valueOf(az)+"\n"
            );*/
            // TODO: add this line to support gyro
            //msg += "gyro:" + statID + String.valueOf(ax) + "," + String.valueOf(ay) + "," + String.valueOf(az) + ",@";
            //Log.i("XUJAY_MM", "gyro:" + statID + String.valueOf(ax) + "," + String.valueOf(ay) + "," + String.valueOf(az) + ",@");
        }


        if (gravity != null && geomag != null) {

            // checks that the rotation matrix is found
            boolean success = SensorManager.getRotationMatrix(inR, I,
                    gravity, geomag);
            if (success) {
                SensorManager.getOrientation(inR, orientVals);
                azimuth = Math.toDegrees(orientVals[0]);
                pitch = Math.toDegrees(orientVals[1]);
                roll = Math.toDegrees(orientVals[2]);
                Log.i(TAG, "azimuth: " + azimuth
                        + ", pitch: " + pitch
                        + ", roll: " + roll);
            }
        }
        if (msg.equalsIgnoreCase("")) return;

        //sendMessage(WEAR_MESSAGE_PATH, msg);
        // TODO: needs to distinguish with high-freq sensors with low-freq sensors, eg. accel vs proximity
        mMessage += msg;
        int speed = FLAG_GAME ? 3 : 1;

        if (statID % speed == 0) {
            try {
                mSharedQueue.put(mMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }

            mMessage = "";
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void sendMessage(final String path, final String text) {
        Log.i(TAG, "Sending message: " + text);
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

                for (Node node: nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }

    class Consumer implements Runnable{

        private final BlockingQueue sharedQueue;

        public Consumer (BlockingQueue sharedQueue) {
            this.sharedQueue = sharedQueue;
        }

        @Override
        public void run() {
            while(true){
                try {
                    String msg = "" + sharedQueue.take();
                    Collection<String> nodes = getNodes();
                    for (String node : nodes)  sendStartActivityMessage(node, msg);

                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }

    private class VibrateThread extends Thread {
        private String message;
        VibrateThread(String msg) {
            message = msg;
        }
        @Override
        public void run() {
            String [] str = message.split(":");
            if (!str[1].matches("[0-9]+")) return;
            int millisec = Integer.parseInt(str[1]);
            Log.d(TAG, "Vibrating for " + millisec);
            mVibrator.vibrate(millisec);
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG,"Received message.~~~~~~~~~~~~~~~~~~~~~~~" + new String(messageEvent.getData()));
        String msg = new String(messageEvent.getData());
        sampleGyro = msg.contains("gyro");
        sampleAccel = msg.contains("accel");
        sampleAmbient = msg.contains("ambient");
        if (msg.contains("vibrator")) {
            VibrateThread vibrateThread = new VibrateThread(msg);
            vibrateThread.start();
        }
    }



    private class StartWearableActivityTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node, args[0]);
            }
            return null;
        }
    }

    @WorkerThread
    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();

        Task<List<Node>> nodeListTask =
                Wearable.getNodeClient(mAct).getConnectedNodes();

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            List<Node> nodes = Tasks.await(nodeListTask);

            for (Node node : nodes) {
                //Log.d(TAG, "getNodes " + node.getDisplayName() + ", geId " + node.getId() );
                results.add(node.getId());
            }

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }
        return results;
    }

    @WorkerThread
    private void sendStartActivityMessage(String node, String data) {
        String msg = data;
        Log.i(TAG, "Sending message: " + msg);
        Task<Integer> sendMessageTask =
                Wearable.getMessageClient(mAct).sendMessage(node, WEAR_MESSAGE_PATH, msg.getBytes());
        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            Integer result = Tasks.await(sendMessageTask);
            //Log.d(TAG, "Message sent: " + result);
        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }
    }


}
