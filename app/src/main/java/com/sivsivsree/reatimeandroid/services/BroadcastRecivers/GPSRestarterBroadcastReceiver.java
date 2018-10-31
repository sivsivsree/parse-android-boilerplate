package com.sivsivsree.reatimeandroid.services.BroadcastRecivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.sivsivsree.reatimeandroid.services.GPSService;

public class GPSRestarterBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Service Stops! Oooooooooooooppppssssss!!!!", Toast.LENGTH_SHORT).show();
        context.startService(new Intent(context, GPSService.class));

    }
}
