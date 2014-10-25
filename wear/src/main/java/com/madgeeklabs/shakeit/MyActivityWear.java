package com.madgeeklabs.shakeit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.madgeeklabs.shakeit.models.User;

import org.w3c.dom.Text;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyActivityWear extends Activity implements SensorEventListener {

    private static final String TAG = MyActivityWear.class.getName();
    private TextView mTextView;
    private ImageView mView;
    private long timeStampSec;
    private SensorManager sensorManager;
    private Sensor mSensor;
    private float xLast;
    private long lastTimeStamp;
    private static final float DIFF = (float) 4.5;
    private String nodeId;
    private long CONNECTION_TIME_OUT_MS = 2 * 1000;
    private Integer amountToPay = 0;


    private int qReadings = 10*3;
    private float[] readings = new float[qReadings];
    private int count = 0;
    private MyReceiver myReceiver;
    private Button paymentButton;
    private ImageView confirmedTransaction;
    private TextView amountToPayTextView;
    private RelativeLayout resultHolder;
    private TextView userName;
    private TextView transactionAmount;

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
                mView = (ImageView) stub.findViewById(R.id.imageView1);
                paymentButton = (Button) findViewById(R.id.pay);
                amountToPayTextView = (TextView) findViewById(R.id.amount_to_pay);
                confirmedTransaction = (ImageView) findViewById(R.id.confirmed_transaction);
                resultHolder = (RelativeLayout) findViewById(R.id.result_holder);
                userName = (TextView) findViewById(R.id.name_holder);
                transactionAmount = (TextView) findViewById(R.id.transaction_holder_amount);
                paymentButton.setOnClickListener(new View.OnClickListener() {
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
        intentFilter.addAction(ListenerServiceWear.IMAGE);
        registerReceiver(myReceiver, intentFilter);

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            Log.d(TAG,"recieved broadcast");
            Log.d(TAG,"recieved " +arg1.getAction());
            if(arg1.getAction().equalsIgnoreCase(ListenerServiceWear.IMAGE)){
                Log.d(TAG,"recieved IMAGE");
                byte[] data = arg1.getByteArrayExtra("DATA");
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                mView.setImageBitmap(bitmap);
            }else if(arg1.getAction().equalsIgnoreCase(ListenerServiceWear.MY_ACTION)){
                double amount = arg1.getDoubleExtra("AMOUNT", 0);
                String name = arg1.getStringExtra("NAME");
                Log.d(TAG, "captured through broadcast, money: " + amount + " from: " + name);
                if (arg1.getStringExtra("TYPE").equalsIgnoreCase("receiving")) {
                    transactionAmount.setText("+" + amount + "€");
                    transactionAmount.setTextColor(getResources().getColor(R.color.green));
                } else {
                    transactionAmount.setText("-" + amount + "€");
                    transactionAmount.setTextColor(getResources().getColor(R.color.error_red));
                }

                paymentButton.setVisibility(View.GONE);
                userName.setText(name);
                confirmedTransaction.setVisibility(View.VISIBLE);
                resultHolder.setVisibility(View.VISIBLE);
            }
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
                    User me = null;
                    if (amountToPay == 0) {
                        me = new User("goofyahead", "receiving", amountToPay, String.valueOf(System.currentTimeMillis()));
                    } else {
                        me = new User("goofyahead", "payment", amountToPay, String.valueOf(System.currentTimeMillis()));
                    }
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
                paymentButton.setVisibility(View.GONE);
                amountToPayTextView.setText("" + amountToPay + "€");
                amountToPayTextView.setVisibility(View.VISIBLE);
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
