package com.jamalsafwat.wear2test;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.RemoteInput;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.jamalsafwat.wear2test.MainActivity.EXTRA_VOICE_REPLY;

public class ViewEventActivity extends AppCompatActivity implements
        DataClient.OnDataChangedListener,
        CapabilityClient.OnCapabilityChangedListener{

    TextView tv;

    FloatingActionButton fab;

    private static final String COUNT_KEY = "com.jimmy.key.count";
    private static final String IMAGE_KEY = "profileImage";
    private int count = 0;
    float savedY = 0;

    private static final long TIMEOUT_MS = 6000;
    private static final String TAG = ViewEventActivity.class.getSimpleName();


    LinearLayoutCompat llBottomSheet;
    private FloatingActionButton fab2;

    private Asset profileAsset;
    private ImageView sendDataImg;
    private Task<List<Node>> nodeListTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        tv = (TextView) findViewById(R.id.tvtv);

        llBottomSheet = (LinearLayoutCompat) findViewById(R.id.bottom_sheet);


        sendDataImg = (ImageView) findViewById(R.id.imageView);

        // init the bottom sheet behavior
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);
        // change the state of the bottom sheet
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
//        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // set the peek height
        bottomSheetBehavior.setPeekHeight(240);// 80 * 3 factor

        // set hideable or not
        bottomSheetBehavior.setHideable(false);



        // set callback for changes
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

                savedY = fab2.getY();

                Log.e("fab2 psotion ", "Y at " + savedY);
/*
                // this part hides the button immediately and waits bottom sheet
                // to collapse to show

                if (BottomSheetBehavior.STATE_DRAGGING == newState) {
                    fab.animate().scaleX(0).scaleY(0).setDuration(300).start();
                    fab2.animate().scaleX(0).scaleY(0).setDuration(300).start();
                } else if (BottomSheetBehavior.STATE_COLLAPSED == newState) {
                    fab.animate().scaleX(1).scaleY(1).setDuration(300).start();
                    fab2.animate().scaleX(1).scaleY(1).setDuration(300).start();
                }
*/
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                // view to scale as the sheet slides
                fab.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset).setDuration(0).start();
                fab2.animate().scaleX(1 - slideOffset ).scaleY(1 - slideOffset ).setDuration(0).start();
//                fab2.animate().translationY(1 - slideOffset).setDuration(0).start();
            }
        });


        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab2 = (FloatingActionButton) findViewById(R.id.fabimg);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseCounter();
            }
        });

        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.uranus);
                putDataMapRequest(bitmap);

            }
        });

        getMessageText(getIntent());
    }


    /**
     * Obtain the intent that started this activity by calling
     * Activity.getIntent() and pass it into this method to
     * get the associated voice input string.
     */

    private CharSequence getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            Log.e("[[[[[[[[[[[", remoteInput.getCharSequence(EXTRA_VOICE_REPLY).toString());
            tv.setText(String.format("Your Response : %s", remoteInput.getCharSequence(EXTRA_VOICE_REPLY).toString()));
            return remoteInput.getCharSequence(EXTRA_VOICE_REPLY);
        }
        return null;
    }



    // Create a data map and put data in it
    private void increaseCounter() {

        /*
        Note: The path string is a unique identifier for the data item that allows you to access
         it from either side of the connection. The path must begin with a forward slash.
          If you're using hierarchical data in your app, you should create a path scheme
           that matches the structure of the data.
         */
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/count");
        //Call PutDataMapRequest.getDataMap() to obtain a data map that you can set values on.
        // Set any desired values for the data map using the put...() methods, such as putString().
        putDataMapReq.getDataMap().putInt(COUNT_KEY, ++count);
        //Call PutDataMapRequest.asPutDataRequest() to obtain a PutDataRequest object
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();

        //Call DataApi.putDataItem() to request the system to create the data item.
        Task<DataItem> pendingResult = Wearable.getDataClient(this).putDataItem(putDataReq);




        Toast.makeText(this, "phone: updated counter to: " + count, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onResume() {
        connected();
        super.onResume();

    }


    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onPause() {
        Wearable.getDataClient(this).removeListener(this);
        super.onPause();
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/count") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    updateCount(dataMap.getInt(COUNT_KEY));
                }else if(item.getUri().getPath().equals("/image")){

                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    profileAsset = dataMapItem.getDataMap().getAsset(IMAGE_KEY);
                    new LongOperation().execute();

                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }

    }


    private void updateCount(int c) {

        tv.setText("message from wear count now is: " + c);
        count = c;

    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {

    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    private Bitmap convertFileToBmp(String path, String imgName, int width, int height){
        File sd = Environment.getExternalStorageDirectory();
        File image = new File(sd + path, imgName);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(),bmOptions);
        return  Bitmap.createScaledBitmap(bitmap, width, height,true);

    }


    // Transfer an Asset
    private void putDataRequest(Bitmap bitmap){

//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), bmpResourceID);
        Asset asset = createAssetFromBitmap(bitmap);
        PutDataRequest request = PutDataRequest.create("/image");
        request.putAsset(IMAGE_KEY, asset);
        Wearable.getDataClient(this).putDataItem(request);
    }

    private void putDataMapRequest(Bitmap bitmap){

//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), bmpResourceID);
        Asset asset = createAssetFromBitmap(bitmap);

        PutDataMapRequest dataMap = PutDataMapRequest.create("/image");
        dataMap.getDataMap().putAsset(IMAGE_KEY, asset);
        dataMap.getDataMap().putLong("timestamp", System.currentTimeMillis());

        PutDataRequest request = dataMap.asPutDataRequest();

        Task<DataItem> pendingResult = Wearable.getDataClient(this).putDataItem(request);


    }


    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream  = null;

        Task<DataClient.GetFdForAssetResponse> dataC = Wearable.getDataClient(this).getFdForAsset(asset);

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            DataClient.GetFdForAssetResponse nodes = Tasks.await(dataC);

            InputStream inStrm = nodes.getInputStream();

            return BitmapFactory.decodeStream(inStrm);

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);
        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }

        // decode the stream into a bitmap
        return null;
    }


    private class LongOperation extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {

            Bitmap b = null;
            try {
                b = loadBitmapFromAsset( profileAsset);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return b;
        }

        @Override
        protected void onPostExecute(Bitmap result) {

            if (result != null) sendDataImg.setImageBitmap(result);

            Toast.makeText(ViewEventActivity.this, "bitmap: " + result, Toast.LENGTH_LONG).show();

        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private void connected (){
        //first get all the nodes, ie connected wearable devices.
        nodeListTask =
                Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();

        nodeListTask.addOnCompleteListener(new OnCompleteListener<List<Node>>() {
            @Override
            public void onComplete(@NonNull Task<List<Node>> task) {

                List<Node> nodes = task.getResult();
                for (Node node : nodes) {
                    Log.e(TAG, "SendThread: message send to " + node.getDisplayName());
                }
            }
        });

        nodeListTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Log.e(TAG, "FFFFFFFFFFFFFail " + e.getLocalizedMessage());
            }
        });

        Wearable.getDataClient(this).addListener(this);

    }


}


