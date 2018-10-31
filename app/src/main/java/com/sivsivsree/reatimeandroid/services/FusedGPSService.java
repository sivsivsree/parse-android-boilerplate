package com.sivsivsree.reatimeandroid.services;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.sivsivsree.reatimeandroid.Xsend;

import org.json.JSONObject;


public class FusedGPSService extends Service implements LocationListener {

    private static final String TAG = FusedGPSService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 1;
    Xsend send;
    private GoogleApiClient mGoogleApiClient;
    private PowerManager.WakeLock mWakelock;
    private Location mLastLocation;
    private long mLastUpdateTimeInMil = 0;
    private GoogleApiClient.ConnectionCallbacks mLocationRequestCallback = new GoogleApiClient.ConnectionCallbacks() {

        @Override
        public void onConnected(Bundle bundle) {
            LocationRequest request = new LocationRequest();
            request.setInterval(10000);
            request.setFastestInterval(5000);
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            if (ActivityCompat.checkSelfPermission(FusedGPSService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                        request, FusedGPSService.this);
                return;
            }


            // Hold a partial wake lock to keep CPU awake when the we're tracking location.
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            mWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
            mWakelock.acquire();
        }

        @Override
        public void onConnectionSuspended(int reason) {
            // TODO: Handle gracefully
        }
    };

    public FusedGPSService() {
        send = new Xsend("driver1");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStart(intent, startId);
        Log.e("Google", "Service Started");
        startLocationTracking();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        // Stop receiving location updates.
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,
                    FusedGPSService.this);
        }
        // Release the wakelock
        if (mWakelock != null) {
            mWakelock.release();
        }
        super.onDestroy();
    }

    /**
     * Starts location tracking by creating a Google API client, and
     * requesting location updates.
     */
    private void startLocationTracking() {


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(mLocationRequestCallback)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        Log.e("Google", "startLocationTracking Started");
    }

    /**
     * Determines if the current location is approximately the same as the location
     * for a particular status. Used to check if we'll add a new status, or
     * update the most recent status of we're stationary.
     */


    private float getBatteryLevel() {
        Intent batteryStatus = registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int batteryLevel = -1;
        int batteryScale = 1;
        if (batteryStatus != null) {
            batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, batteryLevel);
            batteryScale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, batteryScale);
        }
        return batteryLevel / (float) batteryScale * 100;
    }


    @Override
    public void onLocationChanged(Location location) {


        Log.e("Google", "onLocationChanged");

        if (location == null)
            return;


        JSONObject jsonObject = new JSONObject();

        try {

            jsonObject.put("latitude", location.getLatitude());
            jsonObject.put("longitude", location.getLongitude());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                jsonObject.put("speedAccuracy", location.getSpeedAccuracyMetersPerSecond());
            }
            if (mLastLocation != null) {

                jsonObject.put("speed", getSpeed(mLastLocation, location, mLastUpdateTimeInMil));

            } else {
                jsonObject.put("speed", location.getSpeed());
            }
            jsonObject.put("altitude", location.getAltitude());
            jsonObject.put("bearing", location.getBearing());
            jsonObject.put("timestamp", System.currentTimeMillis());

            Log.e("request", jsonObject.toString());

            send.publishMessage(jsonObject.toString());
            mLastLocation = location;
            mLastUpdateTimeInMil = System.currentTimeMillis();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

    private double getSpeed(Location lastLocation, Location currentLastLocation, long lastTime) {
        double speed = ((double) lastLocation.distanceTo(currentLastLocation) / (double) (System.currentTimeMillis() - lastTime));
        speed = (speed * 3600);
        return speed;
    }
}
