package com.jamalsafwat.wear2test;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessaginApiActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
CapabilityApi.CapabilityListener{

    private static final int SPEECH_REQUEST_CODE = 888;
    private static final int MULTIPLE_PERMISSIONS = 600;
    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;
    private String transcriptionNodeId = null;

    private final String TAG = MessaginApiActivity.class.getSimpleName();

    public static final String VOICE_TRANSCRIPTION_MESSAGE_PATH = "/voice_transcription";


    private static final String VOICE_TRANSCRIPTION_CAPABILITY_NAME = "voice_transcription";



    private static final int RECORDING_RATE = 8000; // can go up to 44K, if needed
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_IN, FORMAT);

    private OnVoicePlaybackStateChangedListener mListener;


    private  Handler mHandler;
    private  Context mContext;


    private AsyncTask<Void, Void, Void> mRecordingAsyncTask;

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private int bufferSize;
    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    private Button recBtn;
    private Button sendRecBtn;

    private ImageView sendDataImg;
    private MediaRecorder mediaRecorder;
    private File outputFile;
    private File outputDir;

    private byte[] buffer;
    private byte[] fileBytes;
    private AsyncTask<Void, Void, Void> mPlayingAsyncTask;
    private AudioManager mAudioManager;

    public interface OnVoicePlaybackStateChangedListener {

        /**
         * Called when the playback of the audio file ends. This should be called on the UI thread.
         */
        void onPlaybackStopped();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messagin_api);

        mTextView = (TextView) findViewById(R.id.countr_text_view);
        recBtn = (Button) findViewById(R.id.update_data);
        sendRecBtn = (Button) findViewById(R.id.update_img);
        sendDataImg = (ImageView) findViewById(R.id.img);
        mContext = this;

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);


        recBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //displaySpeechRecognizer();
                askForPermission();
//                requestTranscriptionString(null);
            }
        });

        sendRecBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendDataImg.setImageDrawable(null);
                stopRecording();

                startPlay();
//                requestTranscriptionString(fileBytes);


            }
        });



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
    public void onConnected(@Nullable Bundle bundle) {

        setupVoiceTranscription();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onPause() {
        super.onPause();

        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {

            Log.e(TAG, "onPause");

            Wearable.CapabilityApi.removeCapabilityListener( mGoogleApiClient, this, VOICE_TRANSCRIPTION_CAPABILITY_NAME);

            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mGoogleApiClient != null) {
            Log.e(TAG, "onResume");
            mGoogleApiClient.connect();
        }
    }

    private void setupVoiceTranscription() {

       new LongOperation().execute();

        // Set up listeners for capability changes for the voice contract key.
        Wearable.CapabilityApi.addCapabilityListener( mGoogleApiClient, this, VOICE_TRANSCRIPTION_CAPABILITY_NAME);

    }



    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        updateTranscriptionCapability(capabilityInfo);
    }


    private void updateTranscriptionCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();

        transcriptionNodeId = pickBestNodeId(connectedNodes);

        Log.e(TAG + " transcriptionNodeId", transcriptionNodeId  + "");


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



    private class LongOperation extends AsyncTask<String, Void, CapabilityApi.GetCapabilityResult> {

        @Override
        protected CapabilityApi.GetCapabilityResult doInBackground(String... params) {

            CapabilityApi.GetCapabilityResult result = null;
            try {
                 result = Wearable.CapabilityApi.getCapability(mGoogleApiClient, VOICE_TRANSCRIPTION_CAPABILITY_NAME, CapabilityApi.FILTER_REACHABLE).await();


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

    private void requestTranscriptionString(byte[] voiceData) {

        if (transcriptionNodeId != null) {

            Wearable.MessageApi.sendMessage(mGoogleApiClient, transcriptionNodeId, VOICE_TRANSCRIPTION_MESSAGE_PATH, voiceData)
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

    /**
     * this needs to run in background thread or will throw exception on main thread
     * @return
     */
    private Collection<String> broadcastToAllNodes(){

            HashSet<String> results = new HashSet<String>();
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {
                results.add(node.getId());
            }
            return results;

    }



    protected boolean hasMicrophone() {
        PackageManager pmanager = this.getPackageManager();
        return pmanager.hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE);
    }

   /* private void startRecording() {


        try {

             outputDir = getCacheDir(); // context being the Activity pointer
             outputFile = File.createTempFile("demo", "mp4", outputDir);

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(outputFile.getPath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isRecording = true;

        mediaRecorder.start();
    }*/


    /**
     * Starts recording from the MIC.
     */
    public void startRecording() {


        mRecordingAsyncTask = new AsyncTask<Void, Void, Void>() {

            private AudioRecord mAudioRecord;

            @Override
            protected void onPreExecute() {

            }

            @Override
            protected Void doInBackground(Void... params) {
                mAudioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        RECORDING_RATE,
                        CHANNEL_IN, FORMAT,
                        BUFFER_SIZE * 3);
                BufferedOutputStream bufferedOutputStream = null;
                try {

                    Log.e(TAG, "AudioRecord.STATE_INITIALIZED " + (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED));


                    bufferedOutputStream = new BufferedOutputStream(openFileOutput(outputFile.getName(), Context.MODE_PRIVATE));
                     buffer = new byte[BUFFER_SIZE];
                    mAudioRecord.startRecording();
                    while (!isCancelled()) {
                        int read = mAudioRecord.read(buffer, 0, buffer.length);

                        bufferedOutputStream.write(buffer, 0, read);
                    }
                } catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
                    Log.e(TAG, "Failed to record data: " + e);
                } finally {
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                    mAudioRecord.release();
                    mAudioRecord = null;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                mRecordingAsyncTask = null;
            }

            @Override
            protected void onCancelled() {

                mRecordingAsyncTask = null;
            }
        };

        mRecordingAsyncTask.execute();
    }

    private void stopRecording() {
        // stops the recording activity
        if (mRecordingAsyncTask != null) {
            mRecordingAsyncTask.cancel(true);


           /* try {
                FileInputStream fileInputStream = new FileInputStream(outputFile);
                fileInputStream.read(buffer);
                for (int i = 0; i < b.length; i++) {
                    System.out.print((char)b[i]);
                }
            } catch (FileNotFoundException e) {
                System.out.println("File Not Found.");
                e.printStackTrace();
            }
            catch (IOException e1) {
                System.out.println("Error Reading The File.");
                e1.printStackTrace();
            }*/
        }

    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

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
            byte[] bytes;
            try {
                bytes = spokenText.getBytes("UTF-8");

                requestTranscriptionString(bytes);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //String text = new String(bytes, "UTF-8");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    //--------------------------------------------------- multi / single permission logic handling for M / post-M

    private ArrayList<String> permissionsList;

    /**
     * asks fro permission
     */
    private void askForPermission(){

        /**
         * permissions storage and display names
         */
        // string representation names for the permissions
        List<String> permissionsNeeded = new ArrayList<>();

        // list of permission needed at runtime constants
        permissionsList  = new ArrayList<>();
        /**
         * check if permission is permitted, otherwise add to list of requested permissions permission
         */
        if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO)){
            permissionsNeeded.add("RECORD_AUDIO");
        }

        if (permissionsList.size() > 0) {

            if (permissionsNeeded.size() > 0) {

                // 1st time asking permission
                ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]), MULTIPLE_PERMISSIONS);
                return;

            }

            //second time permission request from user for permission that have been denied before
            // this will appear if permission was denied before and it was required again by a certain feature, hence we ask the user to grant permission again
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]), MULTIPLE_PERMISSIONS);
            return;
        }

        sendDataImg.setImageDrawable(ContextCompat.getDrawable(MessaginApiActivity.this, R.drawable.ic_full_cancel));
        startRecording();

    }


    /**
     * check for permission logic if granted or not, add non-granted permission to list
     * @param permissionsList list of permission to ask for grant
     * @param permission permission string aliases
     * @return
     */
    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
        }
        // Check for Rationale Option
        return ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
    }




    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int approvedPerm = 0;
        int deniedRequests = 0;
        switch (requestCode) {

            case MULTIPLE_PERMISSIONS: {
                // for each permission check if the user granted/denied them
                // you may want to group the rationale in a single dialog,
                // this is just an example
                for (int i = 0, len = permissions.length; i < len; i++) {
                    String permission = permissions[i];
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        deniedRequests++;
                        // user rejected the permission, should we show rational?
                        boolean showRationale = shouldShowRequestPermissionRationale( permission );
                        if (!showRationale) {
                            // user denied before AND CHECKED "never ask again"
                            // you can either enable some fall back,
                            // disable features of your app
                            // or open another dialog explaining
                            // again the permission and directing to
                            // the app setting

                          /*
                           // display alert informing the user
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                            // set dialog message
                            String yes_ = "OK";
                            alertDialogBuilder.setTitle("Some Permission(s) denied");
                            alertDialogBuilder.setMessage("Open application settings screen");
                            alertDialogBuilder.setPositiveButton(yes_, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    startInstalledAppDetailsActivity(MainActivity.this);
                                    dialog.cancel();
                                }
                            });
                            alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            alertDialogBuilder.show();
                           */
                        } else if (Manifest.permission.RECORD_AUDIO.equals(permission) ) {
                            // calendar permission denied once
                            // add to preference that this feature has be disabled


                            // user did NOT check "never ask again"
                            // this is a good place to explain the user
                            // why you need the permission and ask if he wants
                            // to accept it (the rationale)
                            showMessageOKCancel("permission", new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MessaginApiActivity.this, permissionsList.toArray(new String[permissionsList.size()]), MULTIPLE_PERMISSIONS);
                                }
                            });

                        }
                    }else if (grantResults[i] == PackageManager.PERMISSION_GRANTED){
                        // update the count of granted permissions
                        approvedPerm++;
                    }
                }
                // if number of approved permission matches the number of permission requests stored in list
                if(approvedPerm == permissions.length){
                    // set the calendar sync feature is enabled and store that in preference

                    sendDataImg.setImageDrawable(ContextCompat.getDrawable(MessaginApiActivity.this, R.drawable.ic_full_cancel));
                    startRecording();
                }
            }
            break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * rational alert dialog to inform user of what the permission is needed for
     * @param message : message to show to user
     * @param okListener : click interface generic implementation to handle user click
     */
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();

    }

    public static byte[] convertFileToByteArray(File f)
    {
        byte[] byteArray = null;
        try
        {
            InputStream inputStream = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024*8];
            int bytesRead =0;

            while ((bytesRead = inputStream.read(b)) != -1)
            {
                bos.write(b, 0, bytesRead);
            }

            byteArray = bos.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return byteArray;
    }

    public void stopPlaying() {
        if (mPlayingAsyncTask != null) {
            mPlayingAsyncTask.cancel(true);
        }
    }


    /**
     * Starts playback of the recorded audio file.
     */
    public void startPlay() {



        if (!new File(getFilesDir(), outputFile.getName()).exists()) {
            // there is no recording to play
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onPlaybackStopped();
                    }
                });
            }
            return;
        }

        Log.e(TAG, "playing and supported speaker " +  speakerIsSupported() + "," + outputFile.getPath() + "," + outputFile.exists());
        final int intSize = AudioTrack.getMinBufferSize(RECORDING_RATE, CHANNELS_OUT, FORMAT);

        mPlayingAsyncTask = new AsyncTask<Void, Void, Void>() {

            private AudioTrack mAudioTrack;

            @Override
            protected void onPreExecute() {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,  mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0 /* flags */);

            }

            @Override
            protected Void doInBackground(Void... params) {

                byte[] ba = convertFileToByteArray(outputFile);
                try {
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDING_RATE,
                            CHANNELS_OUT, FORMAT, intSize, AudioTrack.MODE_STREAM);
                    byte[] buffer = new byte[intSize * 2];
                    FileInputStream in = null;
                    BufferedInputStream bis = null;
                    mAudioTrack.setVolume(AudioTrack.getMaxVolume());
                    mAudioTrack.play();
                    try {
                        in = openFileInput(outputFile.getName());
                        bis = new BufferedInputStream(in);
                        int read;
                        while (!isCancelled() && (read = bis.read(buffer, 0, buffer.length)) > 0) {
                            mAudioTrack.write(buffer, 0, read);
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

                        Log.e(TAG, "mob psycho " + ba.length + "  -  " + ba.hashCode());

                        requestTranscriptionString(ba);
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
                if (mListener != null) {
                    mListener.onPlaybackStopped();
                }

                mPlayingAsyncTask = null;
            }
        };

        mPlayingAsyncTask.execute();
    }


    /**
     * Determines if the wear device has a built-in speaker and if it is supported. Speaker, even if
     * physically present, is only supported in Android M+ on a wear device..
     */
    public final boolean speakerIsSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager packageManager = getPackageManager();
            // The results from AudioManager.getDevices can't be trusted unless the device
            // advertises FEATURE_AUDIO_OUTPUT.
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
                return false;
            }
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    return true;
                }
            }
        }
        return false;
    }
}
