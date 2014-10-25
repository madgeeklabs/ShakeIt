package com.madgeeklabs.shakeit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MyActivity extends Activity implements SensorEventListener{

    private static final String TAG = MyActivity.class.getName();
    private String nodeId;
    private long CONNECTION_TIME_OUT_MS = 2 * 1000;
    private Button takeSelfie;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


    private byte[] compress(Bitmap bi, int maxKiloBytes){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int compressRatio = 90;
        bi.compress(Bitmap.CompressFormat.JPEG, compressRatio, baos);
        byte[] data = baos.toByteArray();
        while((data.length/1024) > maxKiloBytes){
            compressRatio = compressRatio - 10;
            if(compressRatio<1){
                return new byte[0];
            }
            baos = new ByteArrayOutputStream();
            bi.compress(Bitmap.CompressFormat.JPEG, compressRatio, baos);
            data = baos.toByteArray();
        }
        return data;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        Log.d(TAG, "STARTING AND MESSAGING TO THE WEAR");
        getGoogleApiClient(this);
        retrieveDeviceNode();

        takeSelfie = (Button) findViewById(R.id.take_picture);
        
        // Read a Bitmap from Assets
            // Assign the bitmap to an ImageView in this layout

    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
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
                    Log.d(TAG, "I found a: " + nodeId);
                }
                client.disconnect();

                Log.d(TAG, "I found a: " + nodeId);
                Bitmap bitmap = BitmapFactory.decodeResource(MyActivity.this.getResources(), R.drawable.s);
                byte[] toSend = compress(bitmap, 50);
                sendImage(toSend);
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


   private void sendImage(final byte[] imageBytes) {
       final GoogleApiClient client = getGoogleApiClient(this);
       if (nodeId != null) {
           Log.d(TAG, "sending");
           Log.d(TAG, "bytes" + String.valueOf(imageBytes.length));
           new Thread(new Runnable() {
               @Override
               public void run() {
                   client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                   Wearable.MessageApi.sendMessage(client, nodeId, "IMAGE", imageBytes);
                   client.disconnect();
               }
           }).start();
       }else{
           Log.d(TAG, "not sending");

       }

   }

}
