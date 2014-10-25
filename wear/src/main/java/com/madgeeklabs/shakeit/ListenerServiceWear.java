package com.madgeeklabs.shakeit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.madgeeklabs.shakeit.models.User;

import java.util.concurrent.TimeUnit;

/**
 * Created by goofyahead on 10/24/14.
 */
public class ListenerServiceWear extends WearableListenerService {

    private static final String TAG = ListenerServiceWear.class.getName();
    public static final String MY_ACTION = "SHAKED";
    public static final String IMAGE = "IMAGE";
    String nodeId;
    private long CONNECTION_TIME_OUT_MS = 2 * 1000;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        nodeId = messageEvent.getSourceNodeId();
        Log.d(TAG, "**********************************************" + messageEvent.getPath());
        if(messageEvent.getPath().equalsIgnoreCase("IMAGE")){
            byte[] data = messageEvent.getData();
            Log.d(TAG, "getting image");
            Log.d(TAG, "bytes image" + String.valueOf(data.length));
            Intent intent = new Intent();
            intent.setAction(IMAGE);
            intent.putExtra("DATA", data);
            sendBroadcast(intent);

        }else{
            showToast(messageEvent.getPath());
        }
    }

    private void showToast(String message) {

        Gson gson = new Gson();
        User one = gson.fromJson(message, User.class);
        Intent intent = new Intent();
        intent.setAction(MY_ACTION);
        intent.putExtra("AMOUNT", one.getAmmount());
        intent.putExtra("NAME", one.getUsername());
        sendBroadcast(intent);
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