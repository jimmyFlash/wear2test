package com.jamalsafwat.wear2test.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.jamalsafwat.wear2test.MainActivity;
import com.jamalsafwat.wear2test.ViewEventActivity;

import java.util.concurrent.TimeUnit;

public class ListenerService extends WearableListenerService {
    private String nodeId;

    public ListenerService() {
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        nodeId = messageEvent.getSourceNodeId();
        Log.e("ListenerService", "node-id: " + nodeId + ", path: " + messageEvent.getPath() + ", data size: " + messageEvent.getData().length);
      /*
        Intent startIntent = new Intent(this, ViewEventActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startIntent);
        */
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

   /* private void reply(String message) {
        GoogleApiClient client = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
        client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        Wearable.MessageApi.sendMessage(client, nodeId, message, null);
        client.disconnect();
    }
*/
}
