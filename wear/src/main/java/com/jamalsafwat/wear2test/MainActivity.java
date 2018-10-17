package com.jamalsafwat.wear2test;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.ConfirmationOverlay;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.wearable.intent.RemoteIntent;
import com.google.android.wearable.playstore.PlayStoreAvailability;

import java.util.Set;

public class MainActivity extends WearableActivity implements
        CapabilityClient.OnCapabilityChangedListener
{

    private static final String TAG = "MainActivity";

    private static final String WELCOME_MESSAGE = "Welcome to our Wear app!\n\n";

    private static final String CHECKING_MESSAGE =
            WELCOME_MESSAGE + "Checking for Mobile app...\n";

    private static final String MISSING_MESSAGE =
            WELCOME_MESSAGE
                    + "You are missing the required phone app, please click on the button below to "
                    + "install it on your phone.\n";

    private static final String INSTALLED_MESSAGE =
            WELCOME_MESSAGE
                    + "Mobile app installed on your %1$s!\n\nYou can now use MessageApi, "
                    + "DataApi, etc."
                    + "\n \n connected to: %2$s"
                    + "\n\n Lorem ipsum dolor sit amet, eos sonet decore cu, harum soluta vis in, mea et vide nostrum intellegat. Ignota copiosae urbanitas cum id, ipsum mollis in pri. Eos volutpat euripidis ex, his ei euismod sanctus dolores. No quo alii hendrerit. Regione propriae copiosae nec no. Autem ignota constituam has no, feugait albucius cu nec. Ut vel quidam suavitate liberavisse, et saperet dolorem ponderum has.";

    // Name of capability listed in Phone app's wear.xml.
    // IMPORTANT NOTE: This should be named differently than your Wear app's capability.

    // verify_remote_example_wear_app
    // verify_remote_example_phone_app
    private static  String CAPABILITY_PHONE_APP = "verify_remote_example_phone_app";

    // Links to install mobile app for both Android (Play Store) and iOS.
    // TODO: Replace with your links/packages.
    private static final String PLAY_STORE_APP_URI =
            "market://details?id=com.example.android.wearable.wear.wearverifyremoteapp";

    // TODO: Replace with your links/packages.
    private static final String APP_STORE_APP_URI =
            "https://itunes.apple.com/us/app/android-wear/id986496028?mt=8";

    // Result from sending RemoteIntent to phone to open app in play/app store.
    private final ResultReceiver mResultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if (resultCode == RemoteIntent.RESULT_OK) {
                new ConfirmationOverlay().showOn(MainActivity.this);

            } else if (resultCode == RemoteIntent.RESULT_FAILED) {
                new ConfirmationOverlay()
                        .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                        .showOn(MainActivity.this);

            } else {
                throw new IllegalStateException("Unexpected result " + resultCode);
            }
        }
    };

    private TextView mInformationTextView;
    private Button mRemoteOpenButton;

    private Node mAndroidPhoneNodeWithApp;

    private Button mLaunchActivityButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        CAPABILITY_PHONE_APP = getString(R.string.wear_contract_phone);

        setContentView(R.layout.activity_main);
        setAmbientEnabled();// Sets that this activity should remain displayed when the system enters ambient mode.

        // enable / disable bridging of notifications at runtime
      /*  BridgingManager.fromContext(this).setConfig(
                new BridgingConfig.Builder(this, false)
                        // exclude notifications with specific tags (such as foo or bar):
                        .addExcludedTag("foo")
                        .addExcludedTag("bar")
                        .build());*/

        mInformationTextView = (TextView) findViewById(R.id.information_text_view);
        mRemoteOpenButton = (Button) findViewById(R.id.remote_open_button);
        mLaunchActivityButton = (Button) findViewById(R.id.launch_activity);

        mInformationTextView.setMovementMethod(new ScrollingMovementMethod());// enable scrolling in textview
        mInformationTextView.setText(CHECKING_MESSAGE);

        mRemoteOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAppInStoreOnPhone();
            }
        });

        mLaunchActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent viewIntent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(viewIntent);
                finish();
            }
        });
}


    /**
     * activity entered ambient mode
     * @param ambientDetails
     */
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        mInformationTextView.setTextColor(Color.WHITE);// set text color to white
        mInformationTextView.getPaint().setAntiAlias(false);// remove antialiasing
    }

    /**
     *  allows you to update the screen at this recommended frequency. in every 1 min interval 
     */
    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();

        // TODO: 7/17/2017 update ui in ambient every 1 min.
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        mInformationTextView.setTextColor(Color.YELLOW);// set text color to BLACK
        mInformationTextView.getPaint().setAntiAlias(true);// restore antialiasing

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        Wearable.getCapabilityClient(this)
                .removeListener(this, CAPABILITY_PHONE_APP);

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        connected();

    }


    private void connected (){
        Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("phone://"), CapabilityClient.FILTER_REACHABLE);

        checkIfPhoneHasApp();


    }


    /*
     * Updates UI when capabilities change (install/uninstall phone app).
     */
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "onCapabilityChanged(): " + capabilityInfo);

        mAndroidPhoneNodeWithApp = pickBestNodeId(capabilityInfo.getNodes());
        verifyNodeAndUpdateUI();

    }

    private void checkIfPhoneHasApp() {
        Log.d(TAG, "checkIfPhoneHasApp()");

       Task<CapabilityInfo> pendingResult =   Wearable.getCapabilityClient(this)
               .getCapability(CAPABILITY_PHONE_APP,
                        CapabilityClient.FILTER_REACHABLE);

       pendingResult.addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
           @Override
           public void onComplete(@NonNull Task<CapabilityInfo> task) {

               if(task.isSuccessful()){

                   CapabilityInfo capabilityInfo = task.getResult();
                   mAndroidPhoneNodeWithApp = pickBestNodeId(capabilityInfo.getNodes());
                   verifyNodeAndUpdateUI();
               }else{
                   Log.d(TAG, "Failed CapabilityApi: " + task.getResult());
               }
           }
       });

    }

    private void verifyNodeAndUpdateUI() {

        if (mAndroidPhoneNodeWithApp != null && mAndroidPhoneNodeWithApp.isNearby()) {

            // TODO: Add your code to communicate with the phone app via
            // Wear APIs (MessageApi, DataApi, etc.)

            String installMessage = String.format(INSTALLED_MESSAGE, mAndroidPhoneNodeWithApp.getDisplayName(), mAndroidPhoneNodeWithApp.toString());
            Log.d(TAG, installMessage);
            mInformationTextView.setText(installMessage);
            mRemoteOpenButton.setVisibility(View.GONE);
            mLaunchActivityButton.setVisibility(View.VISIBLE);

        } else if(mAndroidPhoneNodeWithApp != null && !mAndroidPhoneNodeWithApp.isNearby()){
            Log.d(TAG, MISSING_MESSAGE);
            mInformationTextView.setText("phone is not connected to wear, plz connect your phone and try again");
            mRemoteOpenButton.setVisibility(View.GONE);
            mLaunchActivityButton.setVisibility(View.GONE);
        } else {
            Log.d(TAG, MISSING_MESSAGE);
            mInformationTextView.setText(MISSING_MESSAGE);
            mRemoteOpenButton.setVisibility(View.VISIBLE);
            mLaunchActivityButton.setVisibility(View.GONE);
        }
    }

    private void openAppInStoreOnPhone() {
        Log.d(TAG, "openAppInStoreOnPhone()");

        int playStoreAvailabilityOnPhone = PlayStoreAvailability.getPlayStoreAvailabilityOnPhone(getApplicationContext());

        switch (playStoreAvailabilityOnPhone) {

            // Android phone with the Play Store.
            case PlayStoreAvailability.PLAY_STORE_ON_PHONE_AVAILABLE:
                Log.d(TAG, "\tPLAY_STORE_ON_PHONE_AVAILABLE");

                // Create Remote Intent to open Play Store listing of app on remote device.
                Intent intentAndroid =
                        new Intent(Intent.ACTION_VIEW)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                .setData(Uri.parse(PLAY_STORE_APP_URI));

                RemoteIntent.startRemoteActivity(
                        getApplicationContext(),
                        intentAndroid,
                        mResultReceiver);
                break;

            // Assume iPhone (iOS device) or Android without Play Store (not supported right now).
            case PlayStoreAvailability.PLAY_STORE_ON_PHONE_UNAVAILABLE:
                Log.d(TAG, "\tPLAY_STORE_ON_PHONE_UNAVAILABLE");

                // Create Remote Intent to open App Store listing of app on iPhone.
                Intent intentIOS =
                        new Intent(Intent.ACTION_VIEW)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                .setData(Uri.parse(APP_STORE_APP_URI));

                RemoteIntent.startRemoteActivity(
                        getApplicationContext(),
                        intentIOS,
                        mResultReceiver);
                break;

            case PlayStoreAvailability.PLAY_STORE_ON_PHONE_ERROR_UNKNOWN:
                Log.d(TAG, "\tPLAY_STORE_ON_PHONE_ERROR_UNKNOWN");
                break;
        }
    }

    /*
     * There should only ever be one phone in a node set (much less w/ the correct capability), so
     * I am just grabbing the first one (which should be the only one).
     */
    private Node pickBestNodeId(Set<Node> nodes) {
        Log.d(TAG, "pickBestNodeId(): " + nodes);

        Node bestNodeId = null;
        // Find a nearby node/phone or pick one arbitrarily. Realistically, there is only one phone.
        for (Node node : nodes) {
            bestNodeId = node;
        }
        return bestNodeId;
    }


}
