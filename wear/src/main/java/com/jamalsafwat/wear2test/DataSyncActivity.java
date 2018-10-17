package com.jamalsafwat.wear2test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
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
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class DataSyncActivity extends WearableActivity implements
        DataClient.OnDataChangedListener{

    private static final long TIMEOUT_MS = 6000;
    private TextView mTextView;

    private static final String COUNT_KEY = "com.jimmy.key.count";
    private static final String IMAGE_KEY = "profileImage";
    private int count = 0;

    private Button sendDataBtn;
    private static ImageView sendDataImg;
    private static Asset profileAsset;
    private Button sendImgBtn;
    private static final String TAG = DataSyncActivity.class.getSimpleName();
    private Task<List<Node>> nodeListTask;


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
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

        Log.e("WWWWWWWWWWWWWWWWWWWWWw", "onDataChanged");
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().equals("/count")) {

                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                    // update the counter after successful transfer of count data
                    updateCount(dataMap.getInt(COUNT_KEY));

                }else if(item.getUri().getPath().equals("/image")){
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                     profileAsset = dataMapItem.getDataMap().getAsset(IMAGE_KEY);

                    new LongOperation(this).execute();
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
                Log.e("WWWWWWWWWWWWWWWWWWWWWw", "DataEvent.TYPE_DELETED");
            }
        }
    }

    // Our method to update the count display
    private void updateCount(int c) {
        count = c;
        mTextView.setText(String.format(getString(R.string.message_cnt), count));

    }

    private void connected (){
        //first get all the nodes, ie connected wearable devices.
        nodeListTask =   Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();

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

    @Override
    protected void onPause() {
        Wearable.getDataClient(this).removeListener(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // check fo connected devices
        connected();
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

        //Call putDataItem() to request the system to create the data item.
        Task<DataItem> wearData = Wearable.getDataClient(DataSyncActivity.this).putDataItem(putDataReq);

        wearData.addOnCompleteListener(new OnCompleteListener<DataItem>() {
            @Override
            public void onComplete(@NonNull Task<DataItem> task) {
                Log.e("COUNTER - DATA", "integer data request SUCCESS");

            }
        });

        wearData.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("COUNTER - DATA", "integer data request FAIL");
            }
        });

        Toast.makeText(this, "wear: updated counter to: " + count, Toast.LENGTH_LONG).show();
    }


    private static Bitmap loadBitmapFromAsset(Asset asset, Context ctx) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }


        // convert asset into a file descriptor and block until it's ready
        Task<DataClient.GetFdForAssetResponse> assetInputStream = Wearable.getDataClient(ctx)
                .getFdForAsset(asset);
                //.await().getInputStream();


        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            DataClient.GetFdForAssetResponse nodes = Tasks.await(assetInputStream);

            InputStream inStrm = nodes.getInputStream();

            return BitmapFactory.decodeStream(inStrm);

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);
        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }

         return null;
    }


    static class LongOperation extends AsyncTask<String, Void, Bitmap> {

        private WeakReference<DataSyncActivity> activityReference;

        // only retain a weak reference to the activity
        public LongOperation(DataSyncActivity context) {
            activityReference =  new WeakReference<>(context);
        }

        @Override
        protected Bitmap doInBackground(String... params) {

            Bitmap b = null;
            try {
                b = loadBitmapFromAsset( profileAsset, activityReference.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return b;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) sendDataImg.setImageBitmap(result);

            // get a reference to the activity if it is still there
            DataSyncActivity activity = activityReference.get();
            if(activity != null) Toast.makeText(activity, "bitmap: " + result, Toast.LENGTH_LONG).show();
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
//        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
        Task<DataItem> wearDataBmp = Wearable.getDataClient(DataSyncActivity.this).putDataItem(request);

        wearDataBmp.addOnCompleteListener(new OnCompleteListener<DataItem>() {
            @Override
            public void onComplete(@NonNull Task<DataItem> task) {
                Log.e("BITMAP - DATA", "bitmap data request SUCCESS");

            }
        });

        wearDataBmp.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Log.e("BITMAP - DATA", "bitmap data request FAIL");

            }
        });



    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    //This actually sends the message to the wearable device.
    class SendThread extends Thread{

        String path;
        String message;

        public SendThread(String p, String msg) {
            path = p;
            message = msg;
        }
        @Override
        public void run() {
            //first get all the nodes, ie connected wearable devices.
            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();

            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                List<Node> nodes = Tasks.await(nodeListTask);
                for (Node node : nodes) {
                    Log.e(TAG, "SendThread: message send to " + node.getDisplayName());
                }
            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);
            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
            }
        }
    }



}
