package com.madgeeklabs.shakeit;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyActivity extends Activity implements SensorEventListener{

    private static final String TAG = MyActivity.class.getName();
    private TextView mTextView;
    private long timeStampSec;
    private SensorManager sensorManager;
    private Sensor mSensor;
    private float xLast;
    private long lastTimeStamp;
    private static final float DIFF = (float) 1.5;
    private Socket socket;
    private String nodeId;
    private long CONNECTION_TIME_OUT_MS = 2 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);

                getGoogleApiClient(MyActivity.this);
                retrieveDeviceNode();


            }
        });

        Log.d(TAG, "called connect");
    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    private void sendToast() {
        final GoogleApiClient client = getGoogleApiClient(this);
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    Wearable.MessageApi.sendMessage(client, nodeId, "HOLA", null);
                    client.disconnect();
                }
            }).start();
        }
    }

    private void retrieveDeviceNode() {
        final GoogleApiClient client = getGoogleApiClient(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(client).await();
                List<Node> nodes = result.getNodes();
                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();
                }
                client.disconnect();

                Log.d(TAG, "I found a: " + nodeId);
                sendToast();
            }
        }).start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

        // if (x > DIFF) {
        // xText.setText("" + x);
        // yText.setText("" + y);
        // zText.setText("" + z);
        // timeStamp.setText("" + System.currentTimeMillis());
        // Toast.makeText(this, "Fumped! :)", Toast.LENGTH_SHORT).show();
        // }
//        Log.d(TAG, "current X: " + x);
        timeStampSec = System.currentTimeMillis();
        if (x - xLast > DIFF && (timeStampSec - lastTimeStamp) > 800) {
            Log.d(TAG, "" + (x - xLast));
            Toast.makeText(this, "Tapped! :)", Toast.LENGTH_SHORT).show();
            lastTimeStamp = timeStampSec;
        }

        xLast = x;
//        yLast = y;
//        zLast = z;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            getAccelerometer(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
