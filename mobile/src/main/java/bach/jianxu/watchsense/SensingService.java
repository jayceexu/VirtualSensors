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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SensingService extends Service implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        MessageApi.MessageListener,
        SensorEventInjector {

    final static String TAG = "WatchSense";
    private GoogleApiClient mGoogleApiClient;
    private static SensorManager mSensorManager;
    private LinkedBlockingQueue<String> mQueue = new LinkedBlockingQueue<>(100);

    private static final String MESSAGE = "/message";
    private static final String SEPARATOR = "||";
    private static int id = 1;
    private static int MAX_SIZE = 50000;
    private static Sensor mSensor;

    private Socket mSocket;
    private OutputStream mOutputStream;
    private PrintWriter mPrintWriter;

    private boolean empty;

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
                        sendMessage(MESSAGE, "echo back of heart beats");
                        Log.i(TAG, "Sending echo messages");
                        continue;
                    }
                    String [] str = msg.split("@");
                    Log.d(TAG, "msg has been split into " + str.length + " pieces");
                    for (int i = 0; i < str.length; ++i) {
                        Log.d(TAG, "msg is " + str[i]);
                        sendMsgToLocalServer(str[i]);
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
    });

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
        empty = true;

        //new AThread().execute();
        mThread.start();
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
                //msg = mQueue.take();
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
        Log.i(TAG, "onMessageReceived msg: " + new String(messageEvent.getData()));
        // Receiving the motion sensor data
        String message = new String(messageEvent.getData());
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
