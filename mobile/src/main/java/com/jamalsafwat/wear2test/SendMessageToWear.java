package com.jamalsafwat.wear2test;


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
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.UnsupportedEncodingException;
import java.util.Set;

public class SendMessageToWear extends AppCompatActivity  implements  GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MessageApi.MessageListener,
        CapabilityApi.CapabilityListener{

    private static final String TAG = SendMessageToWear.class.getSimpleName();
    private static final String MESSAGE_CAPABILITY_NAME = "message_capable";
    private static final String MESSAGE_CAPABILITY_PATH_1 = "/message_capable_1";
    private static final String MESSAGE_CAPABILITY_PATH_2 = "/message_capable_2";
    private GoogleApiClient mGoogleApiClient;
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
                try {
                    sendToConnectedNodeMsg_2(strBytes.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();

      /*
      FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        /*
        To call the Data Layer API, create an instance of GoogleApiClient,
        the main entry point for any of the Google Play services APIs.
        */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API) // Request access only to the Wearable API
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient != null) {
            Log.e(TAG, "onResume");
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
            Log.e(TAG, "onPause");

            Wearable.CapabilityApi.removeCapabilityListener( mGoogleApiClient, this, MESSAGE_CAPABILITY_NAME);

            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        new LongOperation().execute();
        // Set up listeners for capability changes for the voice contract key.
        Wearable.CapabilityApi.addCapabilityListener( mGoogleApiClient, this, MESSAGE_CAPABILITY_NAME);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }

    private class LongOperation extends AsyncTask<String, Void, CapabilityApi.GetCapabilityResult> {

        @Override
        protected CapabilityApi.GetCapabilityResult doInBackground(String... params) {

            CapabilityApi.GetCapabilityResult result = null;
            try {
                result = Wearable.CapabilityApi.getCapability(mGoogleApiClient, MESSAGE_CAPABILITY_NAME, CapabilityApi.FILTER_REACHABLE).await();


            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(CapabilityApi.GetCapabilityResult result) {

            Log.e(TAG + " result-Capability", result.getCapability().getName());

            updateTranscriptionCapability(result.getCapability());
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private void updateTranscriptionCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();

        connectedNodeID = pickBestNodeId(connectedNodes);

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

    private void sendToConnectedNodeMsg_1(byte[] voiceData) {
        if (connectedNodeID != null) {

            Wearable.MessageApi.sendMessage(mGoogleApiClient, connectedNodeID, MESSAGE_CAPABILITY_PATH_1, voiceData)
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                // Failed to send message
                                Log.e(TAG + " Failed to send", sendMessageResult.getStatus().getStatusMessage());
                            }else{
                                Log.e(TAG + " Sent", sendMessageResult.getStatus().getStatusMessage());
                            }
                        }
                    });
        } else {
            // Unable to retrieve node with transcription capability
            Log.e(TAG + " No node", "Unable to retrieve node");
        }
    }

    private void sendToConnectedNodeMsg_2(byte[] voiceData) {
        if (connectedNodeID != null) {

            Wearable.MessageApi.sendMessage(mGoogleApiClient, connectedNodeID, MESSAGE_CAPABILITY_PATH_2, voiceData)
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                // Failed to send message
                                Log.e(TAG + " Failed to send", sendMessageResult.getStatus().getStatusMessage());
                            }else{
                                Log.e(TAG + " Sent", sendMessageResult.getStatus().getStatusMessage());
                            }
                        }
                    });
        } else {
            // Unable to retrieve node with transcription capability
            Log.e(TAG + " No node", "Unable to retrieve node");
        }
    }
}
