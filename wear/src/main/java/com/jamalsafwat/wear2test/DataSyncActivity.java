package com.jamalsafwat.wear2test;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;


public class DataSyncActivity extends WearableActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final long TIMEOUT_MS = 6000;
    private TextView mTextView;

    private static final String COUNT_KEY = "com.jimmy.key.count";
    private static final String IMAGE_KEY = "profileImage";
    private int count = 0;

    private GoogleApiClient mGoogleApiClient;

    private Button sendDataBtn;
    private ImageView sendDataImg;
    private Asset profileAsset;
    private Button sendImgBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_sync);

        mTextView = (TextView) findViewById(R.id.countr_text_view);
        sendDataBtn = (Button) findViewById(R.id.update_data);
        sendImgBtn = (Button) findViewById(R.id.update_img);
        sendDataImg = (ImageView) findViewById(R.id.img);

        sendDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               increaseCounter();
            }
        });

        sendImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.earth);

                putDataMapRequest(bitmap);

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

        Log.e("WWWWWWWWWWWWWWWWWWWWWw", "onConnected");
        Wearable.DataApi.addListener(mGoogleApiClient, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

        Log.e("WWWWWWWWWWWWWWWWWWWWWw", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.e("WWWWWWWWWWWWWWWWWWWWWw", "onConnectionFailed");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

        Log.e("WWWWWWWWWWWWWWWWWWWWWw", "onDataChanged");
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

//                    Bitmap bitmap = loadBitmapFromAsset(profileAsset);
//                    Log.e("received bitmap", bitmap.toString());
                   // sendDataImg.setImageBitmap(bitmap);

                    new LongOperation().execute();

                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
                Log.e("WWWWWWWWWWWWWWWWWWWWWw", "DataEvent.TYPE_DELETED");
            }
        }

    }

    // Our method to update the count
    private void updateCount(int c) {

        mTextView.setText("message updated from phone count now is : " + c);
        count = c;
    }


    @Override
    protected void onPause() {
        super.onPause();

        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {

            Log.e("WWWWWWWWWWWWWWWWWWWWWw", "onPause");
            Wearable.DataApi.removeListener(mGoogleApiClient, this);

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
            Log.e("WWWWWWWWWWWWWWWWWWWWWw", "onResume");
            mGoogleApiClient.connect();
        }
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
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);

        Toast.makeText(this, "wear: updated counter to: " + count, Toast.LENGTH_LONG).show();
    }


    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result = mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset).await().getInputStream();
//        mGoogleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w("error", "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
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

            Toast.makeText(DataSyncActivity.this, "bitmap: " + result, Toast.LENGTH_LONG).show();

        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }



    private void putDataMapRequest(Bitmap bitmap){

//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), bmpResourceID);
        Asset asset = createAssetFromBitmap(bitmap);

        PutDataMapRequest dataMap = PutDataMapRequest.create("/image");
        dataMap.getDataMap().putAsset(IMAGE_KEY, asset);
        dataMap.getDataMap().putLong("timestamp", System.currentTimeMillis());

        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);


    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }



}
