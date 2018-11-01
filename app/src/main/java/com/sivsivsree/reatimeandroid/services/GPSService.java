package com.sivsivsree.reatimeandroid.services;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sivsivsree.reatimeandroid.MainActivity;
import com.sivsivsree.reatimeandroid.R;
import com.sivsivsree.reatimeandroid.Xsend;

import org.json.JSONObject;

public class GPSService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final int FOREGROUND_SERVICE_ID = 1;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    String STATUS_INTENT = "status";

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
                    setStatusMessage("Speed: "+ getSpeed(mLastLocation, location, mLastUpdateTimeInMil) + " km/hr");

                } else {
                    jsonObject.put("speed", location.getSpeed());
                    setStatusMessage("Speed: "+ location.getSpeed() + " km/hr");
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
        send = new Xsend("driver2");
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
        buildNotification();
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
        return  ((double)((int)(speed *100.0)))/100.0;
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

    private void buildNotification() {
        String NOTIFICATION_CHANNEL_ID = "com.sivsivsree.reatimeandroid.services";
        String channelName = "gps_service";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);
        }


        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID )
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentTitle(getString(R.string.app_name))
                .setOngoing(true)
                .setContentIntent(resultPendingIntent);
        startForeground(FOREGROUND_SERVICE_ID, mNotificationBuilder.build());
    }

    /**
     * Sets the current status message (connecting/tracking/not tracking).
     */
    private void setStatusMessage(String string) {

        mNotificationBuilder.setContentText(string);
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());

        // Also display the status message in the activity.
        Intent intent = new Intent(STATUS_INTENT);
        intent.putExtra(string, 1);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}