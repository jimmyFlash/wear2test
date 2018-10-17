package com.jamalsafwat.wear2test;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.input.WearableButtons;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jamalsafwat.wear2test.adapters.IconAdapter;
import com.jamalsafwat.wear2test.adapters.RecyclerItemClickListener;
import com.jamalsafwat.wear2test.pojo.IconData;
import com.jamalsafwat.wear2test.services.MyJobService;

import java.util.List;

public class SecondActivity extends WearableActivity {

    private static final int MY_JOB_ID = 505;
    private TextView mTextView;
    private static final int SPEECH_REQUEST_CODE = 0;
    private WearableRecyclerView mWearableRecyclerView;

    private static final int MESSAGE_CONNECTIVITY_TIMEOUT = 1;
    private final long NETWORK_CONNECTIVITY_TIMEOUT_MS = 10000;

    private final int MIN_BANDWIDTH_KBPS = 320;


    private Handler mHandler = new MyVeryOwnHandler();


    private static class MyVeryOwnHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_CONNECTIVITY_TIMEOUT:
                    // unregister the network
                    releaseNetwork();
                    break;
            }
        }
    }

    IconData[] data = new IconData[] {
            new IconData("Confirmation Timer", android.R.drawable.ic_dialog_alert),
            new IconData("Confirm ok ", android.R.drawable.ic_dialog_dialer),
            new IconData("Failure", android.R.drawable.ic_dialog_email),
            new IconData("Open on Phone", android.R.drawable.ic_dialog_info),
            new IconData("Wear navigation drawer", android.R.drawable.ic_dialog_map),
            new IconData("Number of buttons on wear ", android.R.drawable.ic_media_pause),
            new IconData("Check network availability", android.R.drawable.ic_media_ff),
            new IconData("Start WIFI activity", android.R.drawable.ic_input_get),
            new IconData("Test data API", android.R.drawable.ic_lock_idle_low_battery),
            new IconData("Test Messaging API", android.R.drawable.ic_popup_disk_full),
            new IconData("Listening to phone ", android.R.drawable.ic_popup_reminder),
            new IconData("Speak ", android.R.drawable.ic_media_next)
    };
    
    private static ConnectivityManager mConnectivityManager;
    private static ConnectivityManager.NetworkCallback mNetworkCallback;
    private ConnectivityManager connMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        mWearableRecyclerView = (WearableRecyclerView) findViewById(R.id.recycler_launcher_view);

        // To align the edge children (first and last) with the center of the screen
        mWearableRecyclerView.setEdgeItemsCenteringEnabled(true);

        IconAdapter adapter = new IconAdapter(data);

        CustomScrollingLayoutCallback customScrollingLayoutCallback =  new CustomScrollingLayoutCallback();

        mWearableRecyclerView.setLayoutManager(  new WearableLinearLayoutManager(this, customScrollingLayoutCallback));

        mWearableRecyclerView.setCircularScrollingGestureEnabled(true);
        mWearableRecyclerView.setBezelFraction(0.5f);
        mWearableRecyclerView.setScrollDegreesPerScreen(90);
        mWearableRecyclerView.setAdapter(adapter);


        mWearableRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {

                        Intent intent;
                        switch(position){
                            case 0:

                                intent = new Intent(SecondActivity.this, ConfirmationTimerActivity.class);
                                startActivity(intent);

                            break;
                            case 1:

                                intent = new Intent(SecondActivity.this, ConfirmationActivity.class);
                                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.msg_sent));
                                startActivity(intent);

                            break;
                            case 2:

                                intent = new Intent(SecondActivity.this, ConfirmationActivity.class);
                                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
                                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.msg_not_sent));
                                startActivity(intent);


                            break;
                            case 3:

                                intent = new Intent(SecondActivity.this, ConfirmationActivity.class);
                                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.OPEN_ON_PHONE_ANIMATION);
                                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.msg_open_phone));
                                startActivity(intent);
                            break;
                            case 4:

                                intent = new Intent(SecondActivity.this, DrawerActivity.class);
                                startActivity(intent);
                            break;
                            case 5:
                                multifunctionBtnsPresent();
                            break;
                            
                            case 6:
                                checkNetworkBandwidth();
                            break;

                            case 7:
                                startActivity(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"));
                            break;

                            case 8:

                                intent = new Intent(SecondActivity.this, DataSyncActivity.class);
                                startActivity(intent);

                                break;
                            case 9:

                                intent = new Intent(SecondActivity.this, MessaginApiActivity.class);
                                startActivity(intent);
                                finish();

                                break;
                            case 10:

                                intent = new Intent(SecondActivity.this, ReceiveMessageFromPhone.class);
                                startActivity(intent);
                                finish();

                                break;

                            default:
                                displaySpeechRecognizer();
                        }

                    }
                })
        );
    }


    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            // Do something with spokenText
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void multifunctionBtnsPresent(){

        int count = WearableButtons.getButtonCount(this);

        if (count > 1) {
            Toast.makeText(this, "Number of multi-function buttons: " + count, Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this, "NO multi-function buttons: " + count, Toast.LENGTH_SHORT).show();
        }

     /*   WearableButtons.ButtonInfo buttonInfo =  WearableButtons.getButtonInfo(this, KeyEvent.KEYCODE_STEM_1);

        if (buttonInfo == null) {
            // KEYCODE_STEM_1 is unavailable
        } else {
            // KEYCODE_STEM_1 is present on the device
        }*/
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_STEM_1) {
                // Do stuff
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_STEM_2) {
                // Do stuff
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_STEM_3) {
                // Do stuff
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_NAVIGATE_NEXT) {
                // Do something that advances a user View to the next item in an ordered list.
                return moveToNextItem();
            } else if (keyCode == KeyEvent.KEYCODE_NAVIGATE_PREVIOUS) {
                // Do something that advances a user View to the previous item in an ordered list.
                return moveToPreviousItem();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /** Shows the next item in the custom list. */
    private boolean moveToNextItem() {
        boolean handled = false;

        Toast.makeText(this, "Move to next", Toast.LENGTH_LONG).show();
        // Return true if handled successfully, otherwise return false.
        return handled;
    }

    /** Shows the previous item in the custom list. */
    private boolean moveToPreviousItem() {
        boolean handled = false;
        Toast.makeText(this, "Move to previous", Toast.LENGTH_LONG).show();
        // Return true if handled successfully, otherwise return false.
        return handled;
    }

    private void checkNetworkBandwidth(){


        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = mConnectivityManager.getActiveNetwork();

        if (activeNetwork != null) {
            int bandwidth = mConnectivityManager.getNetworkCapabilities(activeNetwork).getLinkDownstreamBandwidthKbps();

            if (bandwidth < MIN_BANDWIDTH_KBPS) {

                Intent intent = new Intent(SecondActivity.this, ConfirmationActivity.class);
                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Request a high-bandwidth network");
                startActivity(intent);
            } else {
                // You already are on a high-bandwidth network, so start your network request
                Toast.makeText(this, "you are on a high-bandwidth network", Toast.LENGTH_LONG).show();
            }
        }else{

            Toast.makeText(this, "No active network found", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isNetworkConnected() {
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE); // 1
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo(); // 2
        return networkInfo != null && networkInfo.isConnected(); // 3
    }

    private void requestHighBandwidthNetwork(){
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                mHandler.removeMessages(MESSAGE_CONNECTIVITY_TIMEOUT);
                if (mConnectivityManager.bindProcessToNetwork(network)) {
                    // socket connections will now use this network
                } else {
                    // app doesn't have android.permission.INTERNET permission
                }
            }

            @Override
            public void onLost(Network network) {

                // handle network loss
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                int bandwidth =  mConnectivityManager.getNetworkCapabilities(network).getLinkDownstreamBandwidthKbps();

                if (bandwidth < MIN_BANDWIDTH_KBPS) {
                    // handle insufficient network bandwidth
                }
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        mConnectivityManager.requestNetwork(request, mNetworkCallback);

        // time-out the request after a predetermined length of time and release any associated resources.
        mHandler.sendMessageDelayed( mHandler.obtainMessage(MESSAGE_CONNECTIVITY_TIMEOUT), NETWORK_CONNECTIVITY_TIMEOUT_MS);
    }

    // to release network service to save battery and resources
    private static void releaseNetwork(){
        mConnectivityManager.bindProcessToNetwork(null);
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }


    /*
        JobScheduler object activates MyJobService when the following constraints are met:

        Unmetered networking
        Device charging

        You can use the builder method setExtras to attach a bundle of app-specific metadata to the job request.
        When your job executes, this bundle is provided to your job service. Note the MY_JOB_ID value passed to the JobInfo.Builder constructor. This MY_JOB_ID value is an app-provided identifier. Subsequent calls to cancel, and subsequent jobs created with that same value, will update the existing job:
     */
    private void jobSchedularDemo (){
        JobInfo jobInfo = new JobInfo.Builder(MY_JOB_ID, new ComponentName(this, MyJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setRequiresCharging(true)
//                .setExtras(extras)
                .build();
        ((JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE)).schedule(jobInfo);
    }
}
