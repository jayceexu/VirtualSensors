package bach.jianxu.watchsense;


import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventInjector;
import android.hardware.SensorEventListener;

import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;


import com.google.android.gms.common.api.GoogleApiClient;
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
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class SensingService extends Service implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        MessageApi.MessageListener,
        SensorEventInjector {

    final static String TAG = "WatchSense";
    private GoogleApiClient mGoogleApiClient;
    private static SensorManager mSensorManager;
    //private Queue<String> mQueue;
    private BlockingQueue<String> mQueue = new LinkedBlockingQueue<>(MAX_SIZE);

    private static final String MESSAGE = "/message";
    private static final String SEPARATOR = "||";
    private static int id = 1;
    private static int MAX_SIZE = 50000;

    private static Sensor mSensor;
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
        mSensorManager.registerInjector(Sensor.TYPE_ACCELEROMETER, this);

        Log.i(TAG, "Creating..........");
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorReceived(SensorEvent sensorEvent) {
        String str = "";
        for (int i = 0; i < sensorEvent.values.length; ++i) {
            sensorEvent.values[i] -= 1;
            str += sensorEvent.values[i] + ", ";
        }
        Log.i(TAG, "XUJAY_SS onSensorReceived: values " + str);

//            while (mQueue.isEmpty()) {
//                Log.d(TAG, "The queue is empty....");
//                try {
//                    mQueue.wait();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }

            if (mQueue.size() == 0)
                return;
            StringTokenizer tokens;
            String msg = "";
            try {
                msg = mQueue.take();
            } catch (Exception e) {
                e.printStackTrace();
            }
            tokens = new StringTokenizer(msg, ",");

//            for (int i = 0; i <= tokens.countTokens(); ++i) {
//                float t = Float.valueOf(tokens.nextToken());
//                Log.d(TAG, "float is " + t);
//                sensorEvent.values[i] = t;
//            }
            int i = 0;
            str = "";
            while (tokens.hasMoreTokens()) {
                float t = Float.valueOf(tokens.nextToken());
                str += t + ", ";

                sensorEvent.values[i] = t;
                ++i;
            }
            Log.d(TAG, "XUJAY_SS receiving floats are: " + str);
            //mQueue.notifyAll();


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

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG, "onMessageReceived msg: " + new String(messageEvent.getData()));
        // Receiving the motion sensor data
        String message = new String(messageEvent.getData());
        sendMessage(message);
//        synchronized (mQueue) {
//            mQueue.add(message);
//            Log.i(TAG, "Adding msg to the queue");
//        }
    }


    public class AThread extends AsyncTask<Void, Void, Void> {

        private Socket mSocket;
        private PrintWriter mOutput;
        private OutputStream mOut;

        AThread() {

        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.i(TAG, "Starting AThread....");
            try {
                //Replace below IP with the IP of that device in which server socket open.
                //If you change port then change the port number in the server side code also.
                Log.i(TAG, "Message redirecting Thread starting......");
                mSocket = new Socket("127.0.0.1", 14400);

            } catch (IOException e) {
                e.printStackTrace();
            }

            while (true) {
                try {
//                    Thread.sleep(1000);
//                    Log.i(TAG, "SLEEPING EVERY SECOND");
                    synchronized (mQueue) {
                        if (!mQueue.isEmpty()) {
                            Log.d(TAG, "The queue is empty....");
                            String msg = mQueue.take();
                            Log.d(TAG, "poping up the message " + msg);

                            mSocket = new Socket("127.0.0.1", 14400);
                            mOut = mSocket.getOutputStream();
                            mOutput = new PrintWriter(mOut);

                            mOutput.println(msg);
                            mOutput.flush();

                            mOutput.close();
                            mOut.close();
                            mSocket.close();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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

    public class AExecutor implements Executor, SensorEventInjector {

        AExecutor() {

        }
        @Override
        public void execute(@NonNull Runnable runnable) {
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

        @Override
        public void onSensorReceived(SensorEvent sensorEvent) {
            String str = "";
            for (int i = 0; i < sensorEvent.values.length; ++i) {
                sensorEvent.values[i] -= 1;
                str += sensorEvent.values[i] + ", ";
            }
            Log.i(TAG, "onSensorReceived: values " + str);

            synchronized (mQueue) {
                while (mQueue.isEmpty()) {
                    Log.d(TAG, "The queue is empty....");
                    try {
                        mQueue.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                String msg = mQueue.poll();
                StringTokenizer tokens = new StringTokenizer(msg, ",");


                for (int i = 0; i < tokens.countTokens(); ++i) {
                    float t = Float.valueOf(tokens.nextToken());
                    Log.d(TAG, "float is " + t);
                    sensorEvent.values[i] = t;
                }
                mQueue.notifyAll();
            }
        }
    }
}
