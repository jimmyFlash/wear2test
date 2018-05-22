package com.jamalsafwat.wear2test.services;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class WearListenerService extends WearableListenerService {
    private String nodeId;

    public WearListenerService() {
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        nodeId = messageEvent.getSourceNodeId();
        Log.e("ListenerService", "node-id: " + nodeId + ", path: " + messageEvent.getPath() + ", data size: " + messageEvent.getData().length);

    }

}
