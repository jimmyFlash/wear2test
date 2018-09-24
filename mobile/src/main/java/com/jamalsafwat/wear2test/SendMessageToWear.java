package com.jamalsafwat.wear2test;


import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class SendMessageToWear extends AppCompatActivity  implements

        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener{

    private static final String TAG = SendMessageToWear.class.getSimpleName();
    private static final String MESSAGE_CAPABILITY_NAME = "message_capable";
    private static final String MESSAGE_CAPABILITY_PATH_1 = "/message_capable_1";
    private static final String MESSAGE_CAPABILITY_PATH_2 = "/message_capable_2";
    private String connectedNodeID;
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message_to_wear);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

         fragmentManager = getSupportFragmentManager();
         fragmentTransaction = fragmentManager.beginTransaction();

        SendMessageToWearFragment fragment = SendMessageToWearFragment.newInstance(new SendMessageToWearFragment.SendMessageCallBack() {
            @Override
            public void sendMessageToWear(String strBytes) {
                //
                try {
                    sendToConnectedNodeMsg_1(strBytes.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void sendAnotherMessageToWear(String strBytes) {

                new SendThread(strBytes).start();
            }
        });
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();


    }

    private void connected (){
        Wearable.getCapabilityClient(this)
                .addListener(this, MESSAGE_CAPABILITY_NAME);

        findWearDevicesWithCapability();
    }

    private void findWearDevicesWithCapability() {
        Log.d(TAG, "findWearDevicesWithApp()");

        // You can filter this by FILTER_REACHABLE if you only want to open Nodes (Wear Devices)
        // directly connect to your phone.
        Task<CapabilityInfo> pendingResult =   Wearable.getCapabilityClient(this)
                .getCapability(MESSAGE_CAPABILITY_NAME,
                        CapabilityClient.FILTER_REACHABLE);

        pendingResult.addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
            @Override
            public void onComplete(@NonNull Task<CapabilityInfo> task) {
                if(task.isSuccessful()){
                    CapabilityInfo capabilityInfo = task.getResult();
                    Log.e(TAG, "SUCCESSS CapabilityApi: " + task.getResult());

                    updateTranscriptionCapability(task.getResult().getNodes());

                }else{
                    Log.e(TAG, "Failed CapabilityApi: " + task.getResult());
                }
            }
        });

    }


    @Override
    protected void onResume() {

        super.onResume();
        connected();

    }

    @Override
    protected void onPause() {

        super.onPause();
        Wearable.getCapabilityClient(this).removeListener(this);

    }

    @Override
    protected void onStop() {
        super.onStop();
    }




    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {

        Log.e("Messaage", "   nistekta aa found you ya son of bith ");

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }


    private void updateTranscriptionCapability(Set<Node> nodes) {
        connectedNodeID = pickBestNodeId(nodes);

        Log.e(TAG + " transcriptionNodeId", connectedNodeID  + "");
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private void sendToConnectedNodeMsg_1(byte[] msgByteData) {
        if (connectedNodeID != null) {

            Task<Integer> sendMessageTask =
                    Wearable.getMessageClient(SendMessageToWear.this).sendMessage(connectedNodeID,
                            MESSAGE_CAPABILITY_PATH_1,
                            msgByteData);

          sendMessageTask.addOnCompleteListener(new OnCompleteListener<Integer>() {
              @Override
              public void onComplete(@NonNull Task<Integer> task) {

                  Log.e(TAG, "SendThread: message send to " + connectedNodeID);
              }
          });
        } else {
            // Unable to retrieve node with transcription capability
            Log.e(TAG + " No node", "Unable to retrieve node");
        }
    }

    private void sendToConnectedNodeMsg_2(byte[] voiceData) {
        if (connectedNodeID != null) {

            Task<Integer> sendMessageTask =
                    Wearable.getMessageClient(SendMessageToWear.this).sendMessage(connectedNodeID,
                            MESSAGE_CAPABILITY_PATH_2,
                            voiceData);

            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                Integer result = Tasks.await(sendMessageTask);
                Log.e(TAG, "SendThread: message send to " + connectedNodeID);

            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
            }

        } else {
            // Unable to retrieve node with transcription capability
            Log.e(TAG + " No node", "Unable to retrieve node");
        }
    }


    //This actually sends the message to the wearable device.
    class SendThread extends Thread {
        String msg;

        //constructor
        SendThread(String p) {
            msg = p;
        }

        //sends the message via the thread.  this will send to all wearables connected, but
        //since there is (should only?) be one, so no problem.
        public void run() {
            //first get all the nodes, ie connected wearable devices.

            Task<Integer> sendMessageTask =
                    Wearable.getMessageClient(SendMessageToWear.this).sendMessage(connectedNodeID,
                            MESSAGE_CAPABILITY_PATH_2,
                            msg.getBytes());

            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                Integer result = Tasks.await(sendMessageTask);
                Log.e(TAG, "SendThread: message send to " + connectedNodeID);

            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
            }

        }
    }
}
