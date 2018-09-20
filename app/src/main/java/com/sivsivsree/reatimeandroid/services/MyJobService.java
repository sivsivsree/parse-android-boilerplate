package com.sivsivsree.reatimeandroid.services;

import android.support.annotation.NonNull;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;
import com.sivsivsree.reatimeandroid.App;

public class MyJobService extends FrameworkJobSchedulerService {
    @NonNull
    @Override
    protected JobManager getJobManager() {
        return App.getInstance().getJobManager();
    }
}