package com.sivsivsree.reatimeandroid;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.log.CustomLogger;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;
import com.birbit.android.jobqueue.scheduling.GcmJobSchedulerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.parse.Parse;
import com.sivsivsree.reatimeandroid.services.MyGcmJobService;
import com.sivsivsree.reatimeandroid.services.MyJobService;


public class App extends Application {

    private static App instance;
    private JobManager jobManager;

    public App() {
        instance = this;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(
                new Parse.Configuration.Builder(this)
                        .applicationId("APPLICATION_ID")
                        .server("http://192.168.47.153:1337/parse/")
                        .enableLocalDataStore()
                        .build()
        );
        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

        getJobManager();
    }

    private void configureJobManager() {
        Configuration.Builder builder = new Configuration.Builder(getApplicationContext())
                .customLogger(new CustomLogger() {
                    private static final String TAG = "JOBS";

                    @Override
                    public boolean isDebugEnabled() {
                        return true;
                    }

                    @Override
                    public void d(String text, Object... args) {
                        Log.d(TAG, String.format(text, args));
                    }

                    @Override
                    public void e(Throwable t, String text, Object... args) {
                        Log.e(TAG, String.format(text, args), t);
                    }

                    @Override
                    public void e(String text, Object... args) {
                        Log.e(TAG, String.format(text, args));
                    }

                    @Override
                    public void v(String text, Object... args) {

                    }
                })
                .minConsumerCount(1)//always keep at least one consumer alive
                .maxConsumerCount(Configuration.MAX_CONSUMER_COUNT)//up to 3 consumers at a time
                .loadFactor(3)//3 jobs per consumer
                .consumerKeepAlive(120);//wait 2 minute
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.scheduler(FrameworkJobSchedulerService.createSchedulerFor(getApplicationContext(), MyJobService.class), true);
        } else {
            int enableGcm = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
            if (enableGcm == ConnectionResult.SUCCESS) {
                builder.scheduler(GcmJobSchedulerService.createSchedulerFor(getApplicationContext(), MyGcmJobService.class), true);
            }
        }

        jobManager = new JobManager(builder.build());
    }

    public synchronized JobManager getJobManager() {
        if (jobManager == null) {
            configureJobManager();
        }
        return jobManager;
    }

    public static App getInstance() {
        return instance;
    }
}
