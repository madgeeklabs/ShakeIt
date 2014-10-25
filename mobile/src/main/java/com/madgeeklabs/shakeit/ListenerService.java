package com.madgeeklabs.shakeit;

import android.content.Intent;
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
import com.madgeeklabs.shakeit.api.Api;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;

import retrofit.RestAdapter;

/**
 * Created by goofyahead on 10/24/14.
 */
public class ListenerService extends WearableListenerService {

    private static final String TAG = ListenerService.class.getName();
    String nodeId;
    private long CONNECTION_TIME_OUT_MS = 2 * 1000;
    private Socket socket;
    private String urlData = "http://nowfie.com:7000";

    public ListenerService() {
        super();
        Log.d(TAG, "Service started -------------------------------");

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

        socket.connect();
    }


    private static float[] toFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        FloatBuffer fb = buffer.asFloatBuffer();

        float[] floatArray = new float[fb.limit()];
        fb.get(floatArray);


        return floatArray;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        nodeId = messageEvent.getSourceNodeId();
        Log.d(TAG, "*****************************************" + messageEvent.getPath());
        if(messageEvent.getPath().equals("READINGS")){
            byte[] data = messageEvent.getData();
            float[] readings = toFloatArray(data);
            Log.d("readings", String.valueOf(readings));


            try{
                RestAdapter restAdapter = new RestAdapter.Builder()
                        .setEndpoint(urlData)
                        .build();

                Api service = restAdapter.create(Api.class);
                service.readings(readings);

            }catch (Exception e){

            }

            showToast(messageEvent.getPath());
        }else{
            showToast(messageEvent.getPath());
        socket.emit("message", messageEvent.getPath());
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void reply(String message) {
        GoogleApiClient client = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        Wearable.MessageApi.sendMessage(client, nodeId, message, null);
        client.disconnect();
    }

}
