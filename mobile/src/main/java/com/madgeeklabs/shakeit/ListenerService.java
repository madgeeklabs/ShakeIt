package com.madgeeklabs.shakeit;

import android.content.Context;
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
import com.google.gson.Gson;
import com.madgeeklabs.shakeit.api.Api;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
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

        socket.on("shaked", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "received SHAKEEEEEE -_____--____--___--__--_-_-_");
                sendToast(args[0].toString());
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
