package com.madgeeklabs.shakeit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.madgeeklabs.shakeit.api.Api;
import com.madgeeklabs.shakeit.models.UserData;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by goofyahead on 10/24/14.
 */
public class ListenerService extends WearableListenerService {

    private static final String TAG = ListenerService.class.getName();
    String nodeId;
    private long CONNECTION_TIME_OUT_MS = 2 * 1000;
    private String urlImages = "http://nowfie.com";
    private Socket socket;
    private String urlData = "http://nowfie.com:7000";
    private ShakeitSharedPrefs prefs;

    public ListenerService() {
        super();
        Log.d(TAG, "Service started -------------------------------");

        prefs = new ShakeitSharedPrefs(ShakeApplication.getAppContext());

        try {
            socket = IO.socket(urlData);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "connected to MOBase realtime server");
            }
        });

        socket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "received message");
            }
        });

        socket.on("shaked", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "received SHAKEEEEEE -_____--____--___--__--_-_-_");
                Gson gson = new Gson();
                UserData data = gson.fromJson(args[0].toString(), UserData.class);

                RestAdapter restAdapter = new RestAdapter.Builder()
                        .setEndpoint(urlImages)
                        .build();

                Api service = restAdapter.create(Api.class);

                service.getImage(data.getImage(), new Callback<Response>() {
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

                sendToast(args[0].toString());
            }
        });

        socket.connect();
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

    private static float[] toFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        FloatBuffer fb = buffer.asFloatBuffer();

        float[] floatArray = new float[fb.limit()];
        fb.get(floatArray);


        return floatArray;
    }

    private void sendToast(final String message) {
        Log.d(TAG, "sending message ->" + message);
        final GoogleApiClient client = getGoogleApiClient(this);
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    Wearable.MessageApi.sendMessage(client, nodeId, message, null);
                    client.disconnect();
                }
            }).start();
        }
    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        nodeId = messageEvent.getSourceNodeId();
        Log.d(TAG, "*****************************************" + messageEvent.getPath());
        if(messageEvent.getPath().equals("READINGS")){
            byte[] data = messageEvent.getData();
            float[] readings = toFloatArray(data);
            StringBuffer buff = new StringBuffer(readings.length);
            List l = new ArrayList();
            for(float f: readings){
                l.add(Float.valueOf(f));
                buff.append(f) ;
                buff.append(":") ;
            }
            Log.d("readings", buff.toString());


            try{
                RestAdapter restAdapter = new RestAdapter.Builder()
                        .setEndpoint(urlData)
                        .build();

                Api service = restAdapter.create(Api.class);
                service.readings(l);

            }catch (Exception e){

            }

        }else{
            showToast(messageEvent.getPath());
            Gson gson = new Gson();
            UserData data = gson.fromJson(messageEvent.getPath(), UserData.class);
            data.setUsername(prefs.getRegistrationPushId());
            socket.emit("message", gson.toJson(data));
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
