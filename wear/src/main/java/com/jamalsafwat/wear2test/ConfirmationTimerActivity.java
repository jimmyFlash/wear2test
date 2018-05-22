package com.jamalsafwat.wear2test;

import android.app.Activity;
import android.os.Bundle;
import android.support.wear.widget.CircularProgressLayout;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.TextView;

public class ConfirmationTimerActivity extends WearableActivity implements TextView.OnClickListener, CircularProgressLayout.OnTimerFinishedListener{

    private TextView mTextView;
    private CircularProgressLayout mCircularProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation_timer);
        mCircularProgress = (CircularProgressLayout) findViewById(R.id.circular_progress);
        mCircularProgress.setOnTimerFinishedListener(this);
        mCircularProgress.setOnClickListener(this);

        // Two seconds to cancel the action
        mCircularProgress.setTotalTime(6000);
        // Start the timer
        mCircularProgress.startTimer();
    }

    @Override
    public void onClick(View v) {

        if (v.equals(mCircularProgress)) {
            // User canceled, abort the action
            mCircularProgress.stopTimer();
        }

    }

    @Override
    public void onTimerFinished(CircularProgressLayout layout) {
        // User didn't cancel, perform the action
        finish();
    }
}
