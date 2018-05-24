package com.jamalsafwat.wear2test;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.wearable.notifications.BridgingConfig;
import android.support.wearable.notifications.BridgingManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.wearable.intent.RemoteIntent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Checks if the sample's Wear app is installed on remote Wear device(s). If it is not, allows the
 * user to open the app listing on the Wear devices' Play Store.
 */
public class MainActivity extends AppCompatActivity implements  GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener
{

    private static final String TAG = "MainMobileActivity";
    private static final String EXTRA_EVENT_ID = "eventID";

    private int notificationId = 100;

    private static final String WELCOME_MESSAGE = "Welcome to our Mobile app!\n\n";

    private static final String CHECKING_MESSAGE =
            WELCOME_MESSAGE + "Checking for Wear Devices for app...\n";

    private static final String NO_DEVICES =
            WELCOME_MESSAGE
                    + "You have no Wear devices linked to your phone at this time.\n";

    private static final String MISSING_ALL_MESSAGE =
            WELCOME_MESSAGE
                    + "You are missing the Wear app on all your Wear Devices, please click on the "
                    + "button below to install it on those device(s).\n";

    private static final String INSTALLED_SOME_DEVICES_MESSAGE =
            WELCOME_MESSAGE
                    + "Wear app installed on some your device(s) (%s)!\n\nYou can now use the "
                    + "MessageApi, DataApi, etc.\n\n"
                    + "To install the Wear app on the other devices, please click on the button "
                    + "below.\n";

    private static final String INSTALLED_ALL_DEVICES_MESSAGE =
            WELCOME_MESSAGE
                    + "Wear app installed on all your devices (%s)!\n\nYou can now use the "
                    + "MessageApi, DataApi, etc.";

    // Name of capability listed in Wear app's wear.xml.
    // IMPORTANT NOTE: This should be named differently than your Phone app's capability.
    private static  String CAPABILITY_WEAR_APP = "verify_remote_example_wear_app";

    // Links to Wear app (Play Store).
    // TODO: Replace with your links/packages.
    private static final String PLAY_STORE_APP_URI = "market://details?id=com.example.android.wearable.wear.wearverifyremoteapp";


    // Key for the string that's delivered in the action's intent
    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";

    String photoPath = Environment.getExternalStorageDirectory()+"/Pictures/main_welcome_bg.png";

    public static final String VOICE_TRANSCRIPTION_MESSAGE_PATH = "/voice_transcription";

    // Result from sending RemoteIntent to wear device(s) to open app in play/app store.
    private final ResultReceiver mResultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            Log.d(TAG, "onReceiveResult: " + resultCode);

            if (resultCode == RemoteIntent.RESULT_OK) {
                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        "Play Store Request to Wear device successful.",
                        Toast.LENGTH_SHORT);
                toast.show();

            } else if (resultCode == RemoteIntent.RESULT_FAILED) {
                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        "Play Store Request Failed. Wear device(s) may not support Play Store, "
                                + " that is, the Wear device may be version 1.0.",
                        Toast.LENGTH_LONG);
                toast.show();

            } else {
                throw new IllegalStateException("Unexpected result " + resultCode);
            }
        }
    };

    private TextView mInformationTextView;
    private Button mRemoteOpenButton;

    private Set<Node> mWearNodesWithApp;
    private List<Node> mAllConnectedNodes;

    private GoogleApiClient mGoogleApiClient;

    private MediaPlayer mediaPlayer = new MediaPlayer();



    private static final int RECORDING_RATE = 8000; // can go up to 44K, if needed
    private static final int CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private AsyncTask<Void, Void, Void> mPlayingAsyncTask;
    private AudioManager mAudioManager;
    private  String mOutputFileName;
    private File outputDir;
    private File outputFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState == null) {
            CAPABILITY_WEAR_APP = getString(R.string.wear_contract_wear);

            mInformationTextView = (TextView) findViewById(R.id.information_text_view);
            mRemoteOpenButton = (Button) findViewById(R.id.remote_open_button);

            mInformationTextView.setText(CHECKING_MESSAGE);

            mRemoteOpenButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPlayStoreOnWearDevicesWithoutApp();
                }
            });

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            outputDir = getFilesDir(); // context being the Activity pointer
            try {
                outputFile = File.createTempFile("demo", ".pcm", outputDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.d(TAG, "onConnected()");

        // Set up listeners for capability changes (install/uninstall of remote app).
        Wearable.CapabilityApi.addCapabilityListener(
                mGoogleApiClient,
                this,
                CAPABILITY_WEAR_APP);

        Wearable.MessageApi.addListener( mGoogleApiClient, this );

        // Initial request for devices with our capability, aka, our Wear app installed.
        findWearDevicesWithApp();

        // Initial request for all Wear devices connected (with or without our capability).
        // Additional Note: Because there isn't a listener for ALL Nodes added/removed from network
        // that isn't deprecated, we simply update the full list when the Google API Client is
        // connected and when capability changes come through in the onCapabilityChanged() method.
        findAllWearDevices();

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(): connection to location client suspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed(): " + connectionResult);
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {

        Log.d(TAG, "onCapabilityChanged(): " + capabilityInfo);

        mWearNodesWithApp = capabilityInfo.getNodes();

        // Because we have an updated list of devices with/without our app, we need to also update
        // our list of active Wear devices.
        findAllWearDevices();

        verifyNodeAndUpdateUI();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {

            Wearable.CapabilityApi.removeCapabilityListener(
                    mGoogleApiClient,
                    this,
                    CAPABILITY_WEAR_APP);

            Wearable.MessageApi.removeListener( mGoogleApiClient, this );

            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    private void findWearDevicesWithApp() {
        Log.d(TAG, "findWearDevicesWithApp()");

        // You can filter this by FILTER_REACHABLE if you only want to open Nodes (Wear Devices)
        // directly connect to your phone.
        PendingResult<CapabilityApi.GetCapabilityResult> pendingResult =
                Wearable.CapabilityApi.getCapability(
                        mGoogleApiClient,
                        CAPABILITY_WEAR_APP,
                        CapabilityApi.FILTER_ALL);

        pendingResult.setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
            @Override
            public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                Log.d(TAG, "onResult(): " + getCapabilityResult);

                if (getCapabilityResult.getStatus().isSuccess()) {
                    CapabilityInfo capabilityInfo = getCapabilityResult.getCapability();
                    mWearNodesWithApp = capabilityInfo.getNodes();
                    verifyNodeAndUpdateUI();

                } else {
                    Log.d(TAG, "Failed CapabilityApi: " + getCapabilityResult.getStatus());
                }
            }
        });
    }

    private void findAllWearDevices() {
        Log.d(TAG, "findAllWearDevices()");

        PendingResult<NodeApi.GetConnectedNodesResult> pendingResult = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);

        pendingResult.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetConnectedNodesResult getConnectedNodesResult) {

                if (getConnectedNodesResult.getStatus().isSuccess()) {
                    mAllConnectedNodes = getConnectedNodesResult.getNodes();
                    verifyNodeAndUpdateUI();

                } else {
                    Log.d(TAG, "Failed CapabilityApi: " + getConnectedNodesResult.getStatus());
                }
            }
        });
    }

    private void verifyNodeAndUpdateUI() {
        Log.d(TAG, "verifyNodeAndUpdateUI() ----> \n " + "mAllConnectedNodes : " + mAllConnectedNodes + " \nmWearNodesWithApp: "  + mWearNodesWithApp);

        if ((mWearNodesWithApp == null) || (mAllConnectedNodes == null)) {
            Log.d(TAG, "Waiting on Results for both connected nodes and nodes with app");

        } else if (mAllConnectedNodes.isEmpty()) {
            Log.d(TAG, NO_DEVICES);
            mInformationTextView.setText(NO_DEVICES);
            mRemoteOpenButton.setVisibility(View.INVISIBLE);

        } else if (mWearNodesWithApp.isEmpty()) {
            Log.d(TAG, MISSING_ALL_MESSAGE);
            mInformationTextView.setText(MISSING_ALL_MESSAGE);
            mRemoteOpenButton.setVisibility(View.VISIBLE);

//        } else if (!mWearNodesWithApp.containsAll(mAllConnectedNodes) ) {
        } else if (!mAllConnectedNodes.containsAll(mWearNodesWithApp) ) {
            // TODO: Add your code to communicate with the wear app(s) via
            // Wear APIs (MessageApi, DataApi, etc.)

            String installMessage = String.format(INSTALLED_SOME_DEVICES_MESSAGE, mWearNodesWithApp);
            Log.d(TAG, installMessage);
            mInformationTextView.setText(installMessage);
            mRemoteOpenButton.setVisibility(View.VISIBLE);

        } else {
            // TODO: Add your code to communicate with the wear app(s) via
            // Wear APIs (MessageApi, DataApi, etc.)

            String installMessage = String.format(INSTALLED_ALL_DEVICES_MESSAGE, mWearNodesWithApp);

            mInformationTextView.setText(installMessage);
            mRemoteOpenButton.setVisibility(View.INVISIBLE);

            int nearByDevices = 0;
            for (Node s : mWearNodesWithApp) {
                Log.e(TAG, "is nearby "  +s.isNearby() + "  " + s.getDisplayName());
                if(s.isNearby()){
                    nearByDevices++;
                }

            }

            if(nearByDevices > 0){
                // Build intent for notification content
                Intent viewIntent = new Intent(this, ViewEventActivity.class);
                viewIntent.putExtra(EXTRA_EVENT_ID, 1000);
                PendingIntent viewPendingIntent = PendingIntent.getActivity(this, 0, viewIntent, 0);

                // Build an intent for an action to view a map
                Intent mapIntent = new Intent(Intent.ACTION_VIEW);
                Uri geoUri = Uri.parse("geo:0,0?q=" + Uri.encode("City Hall, New York, NY"));
                mapIntent.setData(geoUri);
                PendingIntent mapPendingIntent = PendingIntent.getActivity(this, 0, mapIntent, 0);



                Bitmap bitmap_image = BitmapFactory.decodeResource(this.getResources(), R.drawable.wear);
                Bitmap bitmap_smile = BitmapFactory.decodeResource(this.getResources(), R.drawable.smile);

                Notification secondPageNotification =
                        new NotificationCompat.Builder(this)
                                .setContentTitle("Hours this week")
                                .setContentText("I am the game i want to play \n motorhead ")
                                .build();


                //Android Wear requires a hint to display the reply action inline.
                NotificationCompat.Action.WearableExtender actionExtender =
                        new NotificationCompat.Action.WearableExtender()
                                .setHintLaunchesActivity(true)
                                .setHintDisplayActionInline(true);

                String replyLabel = getString(R.string.quick_actions_week_label);
                String[] replyChoices = getResources().getStringArray(R.array.weekly_quick_actions);
                String resultsKey = getString(R.string.time_entry_reply_quick_action);

                RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY)
                        .setLabel(getString(R.string.reply_label))
                        .setChoices(replyChoices)
//                        .setAllowFreeFormInput(false)

                        .build();

                Intent replyIntent = new Intent(this, ViewEventActivity.class);
                PendingIntent replyPendingIntent = PendingIntent.getActivity(this, 0, replyIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.smile, "reply", replyPendingIntent)
                                .addRemoteInput(remoteInput)
                                .extend(actionExtender)
                                .build();

                // Create a WearableExtender to add functionality for wearables
                NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender()
                                .setHintHideIcon(true)  // remove the app icon from the notification card
                                .setBackground(bitmap_smile)
                                .setContentIcon(R.mipmap.ic_launcher_round)
//                                .setBridgeTag("foo") // bridge tag to exclude this notification from non bridging
//                                .setDismissalId("abc123") //syncing of dismissals is enabled because a globally unique ID is specified for a new notification
//                                .addPage(secondPageNotification)
                                .addAction(action);



                boolean hintHideIcon = wearableExtender.getHintHideIcon();
                Log.e(TAG, "wearable hint on: " + hintHideIcon);

                NotificationCompat.MessagingStyle.Message message = new NotificationCompat.MessagingStyle.Message("sticker", 1, "Jeff")
                        .setData("image/png", Uri.parse(new File(photoPath).toString()));

                Log.e(";;;;;;;", photoPath);


                NotificationCompat.Builder  notificationBuilder =
                        new NotificationCompat.Builder(this)
                                // sample 1:  with big imageor big text  , action button and bg color set
                              /*  .setSmallIcon(android.R.drawable.ic_dialog_alert)
//                                .setLargeIcon(bitmap_image)
                                .setContentTitle("Devices near you found")
                                .setContentText("you have " + nearByDevices + " paired ")
//                                .setStyle(new NotificationCompat.BigTextStyle().bigText("To show additional text in your expanded notification, use the BigTextStyle"))
                                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap_image).setSummaryText("Wear image summery"))
                                .setAutoCancel(true)
                                .setColor(ContextCompat.getColor(this, R.color.dark_red))
                                .addAction(android.R.drawable.ic_dialog_map, "Open map", mapPendingIntent)
                                .setContentIntent(viewPendingIntent);*/

//                                .setStyle(new NotificationCompat.MessagingStyle("Me").addMessage(message));


                                // sample 2 using wearable extender
                                .setContentTitle("New mail from jimmy")
                                .setContentText(" subject :)))))))")
                                .setSmallIcon(android.R.drawable.ic_dialog_email)
                                .extend(wearableExtender);


                // Get an instance of the NotificationManager service
                NotificationManagerCompat notificationManager =  NotificationManagerCompat.from(this);
                // Issue the notification with notification manager.
                notificationManager.notify(notificationId, notificationBuilder.build());


            }else{
                //
            }

        }
    }

    private void openPlayStoreOnWearDevicesWithoutApp() {
        Log.d(TAG, "openPlayStoreOnWearDevicesWithoutApp()");

        // Create a List of Nodes (Wear devices) without your app.
        ArrayList<Node> nodesWithoutApp = new ArrayList<>();

        for (Node node : mAllConnectedNodes) {
            if (!mWearNodesWithApp.contains(node)) {
                nodesWithoutApp.add(node);
            }
        }

        if (!nodesWithoutApp.isEmpty()) {
            Log.d(TAG, "Number of nodes without app: " + nodesWithoutApp.size());

            Intent intent =
                    new Intent(Intent.ACTION_VIEW)
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                            .setData(Uri.parse(PLAY_STORE_APP_URI));

            for (Node node : nodesWithoutApp) {
                RemoteIntent.startRemoteActivity(
                        getApplicationContext(),
                        intent,
                        mResultReceiver,
                        node.getId());
            }
        }
    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        Log.d(TAG, messageEvent.getPath());
        if (messageEvent.getPath().equals(VOICE_TRANSCRIPTION_MESSAGE_PATH)) {
          /*
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("VOICE_DATA", messageEvent.getData());
            startActivity(startIntent);
            */

            try {
                String text = new String( messageEvent.getData(), "UTF-8");

                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        messageEvent.getPath() + " / "  + messageEvent.getData().length,  Toast.LENGTH_SHORT);
                toast.show();

//                playMp3(messageEvent.getData());
                startPlay(messageEvent.getData());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
    }


    private void playMp3(byte[] mp3SoundByteArray) {
        try {
            // create temp file that will hold byte array
            File tempMp3 = File.createTempFile("demo", "pcm", getCacheDir());
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(mp3SoundByteArray);
            fos.close();

            // resetting mediaplayer instance to evade problems
            mediaPlayer.reset();

            // In case you run into issues with threading consider new instance like:
            // MediaPlayer mediaPlayer = new MediaPlayer();

            // Tried passing path directly, but kept getting
            // "Prepare failed.: status=0x1"
            // so using file descriptor instead
            FileInputStream fis = new FileInputStream(tempMp3);
            mediaPlayer.setDataSource(fis.getFD());

            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();
        }
    }



    /**
     * Starts playback of the recorded audio file.
     */
    public void startPlay(final byte[] mp3SoundByteArray) {


        final int intSize = AudioTrack.getMinBufferSize(RECORDING_RATE, CHANNELS_OUT, FORMAT);

        final byte[] mp3SoundByteArray_ = mp3SoundByteArray;

        mPlayingAsyncTask = new AsyncTask<Void, Void, Void>() {


            private AudioTrack mAudioTrack;

            @Override
            protected void onPreExecute() {
                mAudioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                        0 /* flags */);

            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    try {

                        Log.e(TAG, "mob psycho 222222222" + mp3SoundByteArray_.length + "  -  " + mp3SoundByteArray_.hashCode());

                        FileOutputStream fos = new FileOutputStream(outputFile);
                        fos.write(mp3SoundByteArray_);
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mAudioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            RECORDING_RATE,
                            CHANNELS_OUT,
                            FORMAT,
                            intSize,
                            AudioTrack.MODE_STREAM);

                    byte[] buffer = new byte[intSize * 2];

                    FileInputStream in = null;
                    BufferedInputStream bis = null;
                    mAudioTrack.setVolume(AudioTrack.getMaxVolume());
                    mAudioTrack.play();
                    Log.e(TAG, " mAudioTrack.play() " + outputFile.length());
                    try {
                        in = openFileInput(outputFile.getName());
                        in = new FileInputStream(new File(getFilesDir(), outputFile.getName()));
                        bis = new BufferedInputStream(in);
                        int read;
                        while (!isCancelled() && (read = bis.read(buffer, 0, buffer.length)) > 0) {
                            mAudioTrack.write(buffer, 0, read);
                            Log.e(TAG, "mAudioTrack.write " + buffer.length);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to read the sound file into a byte array", e);
                    } finally {
                        try {
                            if (in != null) {
                                in.close();
                            }
                            if (bis != null) {
                                bis.close();
                            }
                        } catch (IOException e) { /* ignore */}

                        mAudioTrack.release();
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to start playback", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                cleanup();
            }

            @Override
            protected void onCancelled() {
                cleanup();
            }

            private void cleanup() {


                mPlayingAsyncTask = null;
            }
        };

        mPlayingAsyncTask.execute();
    }

}
