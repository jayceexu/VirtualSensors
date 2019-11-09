package bach.jianxu.watchsense;


import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Vibrator;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;


import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;


public class SensingService extends Service implements
        SensorEventListener,
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        MessageApi.MessageListener {

    final static String TAG = "WatchSense";
    private GoogleApiClient mGoogleApiClient;
    private static SensorManager mSensorManager;
    private Sensor mAllSensor;
    private LinkedBlockingQueue<String> mQueue = new LinkedBlockingQueue<>(100);

    private static final String MESSAGE = "/message";
    private static final String SEPARATOR = "||";
    private static int id = 1;
    private static int MAX_SIZE = 50000;
    private static Sensor mSensor;

    private ServerSocket mServerSocket;
    private Thread mServerThread;

    private boolean empty;
    private int calibrating = 0;
    private ArrayList<Double> calix = new ArrayList<>();
    private ArrayList<Double> caliy = new ArrayList<>();
    private ArrayList<Double> caliz = new ArrayList<>();
    private ArrayList<Double> mCoordinates = new ArrayList<>(Arrays.asList(0.0, 0.0, 0.0));
    private boolean mInitCoordinate = false;
    public ArrayList<Metaprogram> metaprograms = new ArrayList<>();

    private final int MATRIX_SIZE = 100;
    private MotionDetector mMotionDetector;
    private MultipleLinearRegression mMultipleLinearRegression;
    private ArrayList<ArrayList<Double>> localMatrix = new ArrayList<>(MATRIX_SIZE);
    private ArrayList<ArrayList<Double>> remoteMatrix = new ArrayList<>(MATRIX_SIZE);

    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //mAllSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ALL);

        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //mSensorManager.registerListener(this, mAllSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
        empty = true;

        //new AThread().execute();
        mThread.start();
        mServerThread = new SocketServerThread();
        mServerThread.start();

        loadMetaprogram();
        mMotionDetector = new MotionDetector(this, gestureListener);
        mMultipleLinearRegression = new MultipleLinearRegression();
        try {
            mMotionDetector.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Creating..........");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        mMotionDetector.stop();
        super.onDestroy();
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.i(TAG, "Handling message from the Activity");
                    calibrating = 0;
                    calix.clear(); caliy.clear(); caliz.clear();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    // Should use Thread instead of AsyncTask as this is a long turn workload
    private Thread mThread = new Thread(new Runnable() {
        private Socket mSocket;
        private PrintWriter mOutput;
        private OutputStream mOut;
        private ConcurrentLinkedQueue<String> mLocal = new ConcurrentLinkedQueue<>();
        @Override
        public void run() {
            while (true) {
                    if (!empty) {
                        mQueue.drainTo(mLocal);
                        empty = true;
                        Log.d(TAG, "Drain to local queue, size: " + mLocal.size());
                        continue;
                    }
                    String msg = mLocal.poll();
                    if (msg == null)
                        continue;
                    if (msg.contains("heart")) {
                        String configEcho = "Metaprogram config ";
                        for (String m : metaprograms.get(0).sensors) {
                            configEcho += m + ", ";
                        }
                        sendMessage(MESSAGE, "echo back of heart beats, " + configEcho);
                        Log.i(TAG, "Sending echo messages with configs: " + configEcho);
                        continue;
                    }
                    String [] str = msg.split("@");
                    Log.d(TAG, "msg has been split into " + str.length + " pieces");
                    for (int i = 0; i < str.length; ++i) {
                        Log.d(TAG, "msg is " + str[i]);
                        String msg2 = applyMetaprogram(str[i], metaprograms.get(0));
                        if (!msg2.equalsIgnoreCase(""))
                            sendMsgToLocalServer(msg2);
                    }
            }
        }

        private void sendMsgToLocalServer(String msg) {
            try {
                if (msg != null) {
                    Log.d("XUJAY_TCP", "poping up the message " + msg + ", size:" + mLocal.size());
                    mSocket = new Socket("127.0.0.1", 14400);
                    mOut = mSocket.getOutputStream();
                    mOutput = new PrintWriter(mOut);
                    mOutput.println(msg);
                    mOutput.flush();

                    mOutput.close();
                    mOut.close();
                    mSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String applyMetaprogram(String msg, Metaprogram meta) {
            // msg format "accel:323,-8.9761505,0.61755913,2.8101335,"

            String[] msgs = msg.split(",");
            if (msgs.length != 4) return "";
            String[] headers = msgs[0].split(":");
            if (headers.length != 2) return "";

            String typeStr = headers[0];
            if (!typeStr.contains("accel")) return msg;

            double x = Double.parseDouble(headers[1]);// + meta.data.get(typeStr).get(0);
            double y = Double.parseDouble(msgs[1]);// + meta.data.get(typeStr).get(1);
            double z = Double.parseDouble(msgs[2]);// + meta.data.get(typeStr).get(2);
            // String calibratedMsg = String.format("%f,%f,%f,",x, y, z);
            // Log.i(TAG, "applyMetaprogram " + calibratedMsg);
            // return calibratedMsg;
            if (!mInitCoordinate) {
                mInitCoordinate = true;
                mCoordinates.set(0, x);
                mCoordinates.set(1, y);
                mCoordinates.set(2, z);
            }

//            synchronized (mMotionDetector.recordingData) {
//                mMotionDetector.recordingData[mMotionDetector.dataPos++] = (float)x / mMotionDetector.DATA_NORMALIZATION_COEF;
//                mMotionDetector.recordingData[mMotionDetector.dataPos++] = (float)y / mMotionDetector.DATA_NORMALIZATION_COEF;
//                //recordingData[dataPos++] = event.values[2] / DATA_NORMALIZATION_COEF;
//                if (mMotionDetector.dataPos >= mMotionDetector.recordingData.length) {
//                    mMotionDetector.dataPos = 0;
//                }
//            }
//            // run recognition if recognition thread is available
//            if (mMotionDetector.recognSemaphore.hasQueuedThreads()) mMotionDetector.recognSemaphore.release();
            //msg = typeStr+":" + mCoordinates.get(0)+","+mCoordinates.get(1)+","+mCoordinates.get(2)+",";

            //msg = typeStr +":" + x + "," + y+ "," + z + ",";
            //Log.d("XUJAY_MM", "recev_message: " + msg);

//            if (remoteMatrix.size() < MATRIX_SIZE) {
//                ArrayList<Double> tuple = new ArrayList<>();
//                tuple.add(x);
//                tuple.add(y);
//                tuple.add(z);
//                remoteMatrix.add(tuple);
//                Log.i(TAG, "Calibrating remoteMatrix size " + remoteMatrix.size()
//                                + ", x, y, z: " + x + " " + y + " " + z);
//            } else if (remoteMatrix.size() == MATRIX_SIZE) {
//                mMultipleLinearRegression.calibrate(remoteMatrix, localMatrix);
//                ArrayList<Double> tuple = new ArrayList<>();
//                tuple.add(x);
//                tuple.add(y);
//                tuple.add(z);
//                remoteMatrix.add(tuple);
//                Log.i(TAG, "Finished mMultipleLinearRegression");
//            } else {
//                // Now converting the real data
//                ArrayList<ArrayList<Double>> arr = new ArrayList<>();
//                ArrayList<Double> tuple = new ArrayList<>();
//                tuple.add(x); tuple.add(y); tuple.add(z);
//                arr.add(tuple);
//                ArrayList<ArrayList<Double>> output = mMultipleLinearRegression.convert(arr);
//                if (output.size() < 1) return "";
//                //msg = typeStr+":" + output.get(0).get(0)+","+output.get(0).get(1)+","+output.get(0).get(2)+",";
//            }

            Log.i(TAG, "Calibrating remoteMatrix size " + remoteMatrix.size());
            return msg;
        }

    });

    private final MotionDetector.Listener gestureListener = new MotionDetector.Listener() {
        @Override
        public void onGestureRecognized(MotionDetector.GestureType gestureType) {
            Log.d(TAG, "Gesture detected: " + gestureType);
            double x = mCoordinates.get(0);
            double y = mCoordinates.get(1);
            double d = 5;
            if (gestureType == MotionDetector.GestureType.MoveLeft) {
                //mCoordinates.set(0, x+d);
                //mCoordinates.set(1, y+d);
                if (Shell.isSuAvailable()) {
                    String command = "input swipe 483 1120 930 1120";
                    Log.d(TAG, "Command is: " + command);
                    Shell.runCommand(command);
                }
            } else if (gestureType == MotionDetector.GestureType.MoveRight) {
                //mCoordinates.set(0, x-d);
                //mCoordinates.set(1, y-d);
                if (Shell.isSuAvailable()) {
                    String command = "/data/local/tmp/mysendevent /dev/input/event1 /sdcard/temp/recorded_touch_events.txt";
                    Log.d(TAG, "Command is: " + command);
                    Shell.runCommand(command);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    // Alternative way to create socket for TCP client
    private void sendMessage(final String msg) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    //Replace below IP with the IP of that device in which server socket open.
                    //If you change port then change the port number in the server side code also.
                    Socket s = new Socket("127.0.0.1", 14400);

                    OutputStream out = s.getOutputStream();

                    PrintWriter output = new PrintWriter(out);

                    output.println(msg);
                    output.flush();

                    output.close();
                    out.close();
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    private void sendMessage(final String path, final String text) {
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

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // Receiving the motion sensor data
        String message = new String(messageEvent.getData());
        Log.i(TAG, "onMessageReceived msg: " + message);

        //sendMessage(message);
        //sendMessage2(message);

        try {
            if (empty) {
                mQueue.put(message);
                Log.i("XUJAY_TCP", "Adding msg to the queue, queue size: " + mQueue.size());
            }
            if (mQueue.size() >= 1) {
                empty = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected....");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Connection Suspended", "Connection suspended");
    }

    public void onDataChanged(DataEventBuffer dataEvents) {

    }

    private void loadMetaprogram() {
        try {
            File SDCardRoot = Environment.getExternalStorageDirectory()
                    .getAbsoluteFile();
            File myDir = new File(SDCardRoot.getAbsolutePath() + "/temp/");
            File file = new File(myDir, "config.xml");

            InputStream is = new FileInputStream(file.getPath());

            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, null);
            int eventType = parser.getEventType();
            String text = "";
            String lastSensor = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        break;
                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (name.equalsIgnoreCase("appname")) {
                            Metaprogram metaprogram = new Metaprogram();
                            metaprogram.app_name = text;
                            metaprograms.add(metaprogram);
                        } else if (name.equalsIgnoreCase("op")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size()-1);
                            meta.op = text;

                        } else if (name.equalsIgnoreCase("orientation")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size()-1);
                            meta.orientation = text;

                        } else if (name.equalsIgnoreCase("sensor_type")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size()-1);
                            meta.sensors.add(text);
                            lastSensor = text;
                            meta.data.put(lastSensor, new ArrayList<Double>());

                        } else if (name.equalsIgnoreCase("freq")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size()-1);
                            meta.freqs.put(lastSensor, Integer.parseInt(text));

                        } else if (name.equalsIgnoreCase("x-calibrate")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size()-1);
                            meta.data.get(lastSensor).add(0, Double.parseDouble(text));

                        } else if (name.equalsIgnoreCase("y-calibrate")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size() - 1);
                            meta.data.get(lastSensor).add(1, Double.parseDouble(text));

                        } else if (name.equalsIgnoreCase("z-calibrate")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size() - 1);
                            meta.data.get(lastSensor).add(2, Double.parseDouble(text));
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }

        } catch (Exception e) {
            Log.e(TAG, "XML Pasing Excpetion = " + e);
        }
        for (Metaprogram meta: metaprograms) {
            meta.dump();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

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
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.i(TAG, "sampling from phone device");
        // If the sensor data is unreliable return

        // Gets the value of the sensor that has been changed
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = sensorEvent.values.clone();
                if (localMatrix.size() >= MATRIX_SIZE) return;
                double ax = sensorEvent.values[0];
                double ay = sensorEvent.values[1];
                double az = sensorEvent.values[2];
                ArrayList<Double> tuple = new ArrayList<>();
                tuple.add(ax); tuple.add(ay); tuple.add(az);
                localMatrix.add(tuple);
                Log.i(TAG, "Calibrating localMatrix size " + localMatrix.size()
                        + ", x, y, z: " + ax + " " + ay + " " + az);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomag = sensorEvent.values.clone();
                break;
        }

        // If gravity and geomag have values then find rotation matrix
        if (gravity != null && geomag != null) {
            // checks that the rotation matrix is found
            boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
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
    }

    public class AExecutor implements Executor {

        AExecutor() {

        }
        @Override
        public void execute(Runnable runnable) {
            Log.i(TAG, "Starting AExecutor....");

            while (true) {
                try {
                    Thread.sleep(2000);

                    Log.i(TAG, "Starting AExecutor....");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private class VibrationThread extends Thread {
        String message;
        VibrationThread(String msg) { message = msg; }
        @Override
        public void run() {
            sendMessage(MESSAGE, message);
        }
    }

    private class SocketServerThread extends Thread {

        @Override
        public void run() {
            try {
                // create ServerSocket using specified port
                mServerSocket = new ServerSocket(16600);

                while (true) {
                    // block the call until connection is created and return
                    // Socket object
                    Socket clientSocket = mServerSocket.accept();
                    BufferedReader in =
                            new BufferedReader(
                                    new InputStreamReader(clientSocket.getInputStream()));
                    String msg = in.readLine();
                    Log.d(TAG, "received vibration " + msg);
                    Thread vThread= new VibrationThread(msg);
                    vThread.start();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


}
