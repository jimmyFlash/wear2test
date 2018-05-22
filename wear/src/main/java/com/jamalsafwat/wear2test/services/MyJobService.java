package com.jamalsafwat.wear2test.services;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class MyJobService extends JobService {
    public MyJobService() {
    }

    /*
    he onStartJob method is called on the main application thread, and therefore any expensive logic should be run from a separate thread
     */
    @Override
    public boolean onStartJob(JobParameters params) {
//        JobParameters object enables you to get the job ID value along with any extras bundle provided when scheduling the job

        //work is complete, you would call the jobFinished method to notify JobScheduler that the task is done
       /*
       True if your service needs to process the work (on a separate thread). False if there's no more work to be done for this job.
        */
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }




}
