package com.madgeeklabs.shakeit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.madgeeklabs.shakeit.models.User;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyActivityWear extends Activity implements SensorEventListener {

    private static final String TAG = MyActivityWear.class.getName();
    private TextView mTextView;
    private long timeStampSec;
    private SensorManager sensorManager;
    private Sensor mSensor;
    private float xLast;
    private long lastTimeStamp;
    private static final float DIFF = (float) 4.5;
    private String nodeId;
    private long CONNECTION_TIME_OUT_MS = 2 * 1000;
    private Integer amountToPay;


    private int qReadings = 10*3;
    private float[] readings = new float[qReadings];
    private int count = 0;
    private MyReceiver myReceiver;

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
                final Button button = (Button) findViewById(R.id.pay);
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        displaySpeechRecognizer();
                    }
                });

                getGoogleApiClient(MyActivityWear.this);
                retrieveDeviceNode();
            }
        });

        Log.d(TAG, "called connect");
    }

    @Override
    protected void onResume() {
        super.onResume();
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ListenerServiceWear.MY_ACTION);
        registerReceiver(myReceiver, intentFilter);

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            double datapassed = arg1.getDoubleExtra("AMOUNT", 0);
            Log.d(TAG, "captured through broadcast, money: " + datapassed);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        unregisterReceiver(myReceiver);
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
                    User me = new User("goofyahead", "payment", 16.50, String.valueOf(System.currentTimeMillis()));
                    Gson myGson = new Gson();
                    String message = myGson.toJson(me, User.class);
                    Wearable.MessageApi.sendMessage(client, nodeId, message, null);
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
            }
        }).start();
    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];
        if(false) {
            readings[count] = x;
            count++;
            readings[count] = y;
            count++;
            readings[count] = z;
            count++;
        }
        if(false && count % qReadings == 0){

            //service.readings(readings);
            ByteBuffer byteBuf = ByteBuffer.allocate(4 * readings.length);
            FloatBuffer floatBuf = byteBuf.asFloatBuffer();
            floatBuf.put(readings);
            byte [] byte_array = byteBuf.array();
//            sendReadings(byte_array);
            count = 0  ;
        }

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
            Toast.makeText(this, "" + (x - xLast), Toast.LENGTH_SHORT).show();
            sendToast();
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

   private void sendReadings(final byte[] readings) {
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
           }
       }).start();
       if (nodeId != null) {
           new Thread(new Runnable() {
               @Override
               public void run() {
                   client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                   Wearable.MessageApi.sendMessage(client, nodeId, "READINGS", readings);
                   client.disconnect();
               }
           }).start();
       }
   }



    private static final int SPEECH_REQUEST_CODE = 0;

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            try {
                amountToPay = Integer.parseInt(spokenText);
            } catch (NumberFormatException e){
                if(spokenText.equalsIgnoreCase("cancel")){

                }else{
                    displaySpeechRecognizer();
                }

            }

            Log.d("spoken", spokenText);
            // Do something with spokenText
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
