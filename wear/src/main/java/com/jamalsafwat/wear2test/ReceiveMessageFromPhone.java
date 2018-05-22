package com.jamalsafwat.wear2test;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.io.UnsupportedEncodingException;

public class ReceiveMessageFromPhone extends WearableActivity implements  GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MessageApi.MessageListener {

    private static final String TAG = ReceiveMessageFromPhone.class.getSimpleName();
    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;


    private static final String MESSAGE_CAPABILITY_NAME = "message_capable";
    private static final String MESSAGE_CAPABILITY_PATH_1 = "/message_capable_1";
    private static final String MESSAGE_CAPABILITY_PATH_2 = "/message_capable_2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_message_from_phone);
        mTextView = (TextView) findViewById(R.id.recve_text_view);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Wearable.MessageApi.addListener( mGoogleApiClient, this );

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        Log.d(TAG, messageEvent.getPath());
        if (messageEvent.getPath().equals(MESSAGE_CAPABILITY_PATH_1) || (messageEvent.getPath().equals(MESSAGE_CAPABILITY_PATH_2))) {

            try {
                String text = new String( messageEvent.getData(), "UTF-8");

                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        text,  Toast.LENGTH_SHORT);
                toast.show();

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.removeListener( mGoogleApiClient, this );
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }
}
