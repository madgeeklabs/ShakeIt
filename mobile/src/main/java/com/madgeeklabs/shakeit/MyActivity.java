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
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.madgeeklabs.shakeit.api.Api;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;


public class MyActivity extends Activity implements SensorEventListener{

    private static final String TAG = MyActivity.class.getName();
    private String nodeId;
    private long CONNECTION_TIME_OUT_MS = 2 * 1000;
    private Button takeSelfie;
    private ImageView mImageView;
    private EditText mUsername;
    private String urlData = "http://nowfie.com:7000";
    private String urlImages = "http://nowfie.com";
    private ShakeitSharedPrefs prefs;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);


            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(urlData)
                    .build();

            Api service = restAdapter.create(Api.class);


            File f = Utils.getOutputMediaFile(Utils.MEDIA_TYPE_IMAGE);

            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            byte[] bitmapdata = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
                fos.write(bitmapdata);
                fos.close();
                TypedFile file = new TypedFile("application/octet-stream", f);
                service.uploadSelfie(mUsername.getText().toString(), file, new Callback<Response>() {
                    @Override
                    public void success(Response response, Response response2) {
                       Log.d(TAG,"success") ;
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.d(TAG,"failure") ;
                        Log.d(TAG,error.getMessage()) ;

                    }
                });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


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

        prefs = new ShakeitSharedPrefs(this);
        Log.d(TAG, "STARTING AND MESSAGING TO THE WEAR");
        getGoogleApiClient(this);
        retrieveDeviceNode();

        takeSelfie = (Button) findViewById(R.id.take_picture);
        mImageView = (ImageView) findViewById(R.id.imageView1);
        mUsername = (EditText) findViewById(R.id.username);
        takeSelfie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.setRegistrationPushId(mUsername.getText().toString());
                dispatchTakePictureIntent();
            }
        });
        
        // Read a Bitmap from Assets
            // Assign the bitmap to an ImageView in this layout

    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
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

                            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(urlImages)
                    .build();

            Api service = restAdapter.create(Api.class);

            service.getImage("/shakeit/23116-urxv69.jpg", new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    Log.d(TAG,"success");
                    final Response r = response;
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground( final Void ... params ) {
                            try {
                                InputStream is = null;

                                is = r.getBody().in();
                                byte[] bytes = IOUtils.toByteArray(is);

                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                byte[] toSend = compress(bitmap, 50);
                                sendImage(toSend);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            return null;
                        }

                        @Override
                        protected void onPostExecute( final Void result ) {
                            // continue what you are doing...

                        }
                    }.execute();


                }

                @Override
                public void failure(RetrofitError error) {
                    Log.d(TAG,"failure");
                    Log.d(TAG,error.getMessage());

                }
            });

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
