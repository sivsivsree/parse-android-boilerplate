package com.sivsivsree.reatimeandroid.services;



import android.support.annotation.NonNull;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.scheduling.GcmJobSchedulerService;
import com.sivsivsree.reatimeandroid.App;

/**
 * Created by Siv.
 */
public class MyGcmJobService extends GcmJobSchedulerService {
    @NonNull
    @Override
    protected JobManager getJobManager() {
        return App.getInstance().getJobManager();
    }
}
