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
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.io.UnsupportedEncodingException;

public class ReceiveMessageFromPhone extends WearableActivity implements
        MessageClient.OnMessageReceivedListener  {

    private static final String TAG = ReceiveMessageFromPhone.class.getSimpleName();
    private TextView mTextView;

    private static final String MESSAGE_CAPABILITY_PATH_1 = "/message_capable_1";
    private static final String MESSAGE_CAPABILITY_PATH_2 = "/message_capable_2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_message_from_phone);
        mTextView = (TextView) findViewById(R.id.recve_text_view);

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

                mTextView.setText(text);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getMessageClient(ReceiveMessageFromPhone.this).removeListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getMessageClient(ReceiveMessageFromPhone.this).addListener(this);

    }

}
