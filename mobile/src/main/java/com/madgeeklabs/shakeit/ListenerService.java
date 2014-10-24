package com.madgeeklabs.shakeit;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * Created by goofyahead on 10/24/14.
 */
public class ListenerService extends WearableListenerService {

    private static final String TAG = ListenerService.class.getName();
    String nodeId;
    private long CONNECTION_TIME_OUT_MS = 2 * 1000;
    private Socket socket;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            socket = IO.socket("http://nowfie.com:3111");
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

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        nodeId = messageEvent.getSourceNodeId();
        Log.d(TAG, "*****************************************" + messageEvent.getPath());
        showToast(messageEvent.getPath());
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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