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

import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class SensingService extends Service implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        MessageApi.MessageListener,
        SensorEventInjector,
        SensorEventListener {

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

        //Thread m = new AThread();
        //m.start();
        new AThread().execute();

//       new AExecutor().execute(null);

        Log.i(TAG, "Creating..........");
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

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

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG, "onMessageReceived msg: " + new String(messageEvent.getData()));
        // Receiving the motion sensor data
        String message = new String(messageEvent.getData());

        synchronized (mQueue) {
            while (mQueue.size() == MAX_SIZE) {
                Log.d(TAG, "queue is full, waiting....");
                try {
                    mQueue.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mQueue.add(message);
            mQueue.notifyAll();
        }
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


    public class AThread extends AsyncTask<Void, Void, Void> implements
            DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks,
            MessageApi.MessageListener {

        AThread() {
            mGoogleApiClient = new GoogleApiClient.Builder(SensingService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .build();
            mGoogleApiClient.connect();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.i(TAG, "Starting AThread....");

//            while (true) {
//                try {
//                    Thread.sleep(2000);
//
//                    Log.i(TAG, "Starting AThread....");
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }

            return null;
        }

        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            // Receiving the motion sensor data
            String message = new String(messageEvent.getData());
            try {
                mQueue.put(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(TAG, "onMessageReceived msg: " + new String(messageEvent.getData())
                        + ", queue size: " + mQueue.size());

        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {

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

        //        @Override
//        public void run() {
//            while(true) {
//                //Log.i(TAG, "Running AThread....");
//                if (mHandler.hasMessages(1)) {
//                    Log.i(TAG, "We received this message......");
//
//                }
//            }
//
//        }


    }

}
