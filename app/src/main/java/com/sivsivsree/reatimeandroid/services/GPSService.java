package com.sivsivsree.reatimeandroid.services;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.sivsivsree.reatimeandroid.Xsend;

import org.json.JSONObject;

public class GPSService extends Service {

    PowerManager.WakeLock wakeLock;
    LocationManager locationManager;
    Xsend send;
    private Location mLastLocation;
    private long mLastUpdateTimeInMil = 0;
    private LocationListener listener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            // TODO Auto-generated method stub

            Log.e("Google", "Location Changed");

            if (location == null)
                return;

//            if (isConnectingToInternet(getApplicationContext())) {

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


        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }
    };

    public GPSService() {
        send = new Xsend("driver1");
    }

    public static boolean isConnectingToInternet(Context _context) {
        ConnectivityManager connectivity = (ConnectivityManager) _context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager pm = (PowerManager) getSystemService(this.POWER_SERVICE);

        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoNotSleep");
        wakeLock.acquire();

        Log.e("Google", "Service Created");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStart(intent, startId);

        Log.e("Google", "Service Started");

        locationManager = (LocationManager) getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (locationManager != null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        1000, 0, listener);
            }
        }
        Log.e("Google", "Service Started!!!!!!!!!!");
        return START_STICKY;
    }

    private double getSpeed(Location lastLocation, Location currentLastLocation, long lastTime) {
        double speed = ((double) lastLocation.distanceTo(currentLastLocation) / (double) (System.currentTimeMillis() - lastTime));
        speed = (speed * 3600);
        return speed;
    }

    @Override
    public void onDestroy() {

        //  Intent broadcastIntent = new Intent("BroadcastRecivers.GPSRestarterBroadcastReceiver");
        //  sendBroadcast(broadcastIntent);
        Log.d("Service Log", "Destroyed!");
        if (locationManager != null) {
            locationManager.removeUpdates(listener);
        }
        wakeLock.release();
        super.onDestroy();

    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent);

        super.onTaskRemoved(rootIntent);
    }

}