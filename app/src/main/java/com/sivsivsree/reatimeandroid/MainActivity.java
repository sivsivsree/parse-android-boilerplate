package com.sivsivsree.reatimeandroid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.birbit.android.jobqueue.JobManager;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ProgressCallback;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {


    private static final int PICKFILE_REQUEST_CODE = 0x001;
    int counter = 0;
    String tripID = "TP" + System.currentTimeMillis();
    String[] tevents = {"start", "stop", "running"};
    TextView data;
    Button button, pick;
    boolean activeTask = false;
    Timer timer;
    final Handler h = new Handler();
    JobManager jobManager;
    int j = 0;
    Runnable runnable = new Runnable() {
        private long time = 0;
        int i = 0;

        @Override
        public void run() {
            // do stuff then
            // can call h again after work!

            sendDataFromRabbitMQ();
            reset();

            time += 1000;
            i++;
            Log.d("TimerExample", "Going for... " + time);
            h.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        jobManager = App.getInstance().getJobManager();
        data = findViewById(R.id.data);
        button = findViewById(R.id.button);
        pick = findViewById(R.id.pick);
        reset();

    }

    public void addToOfflineQueue(View view) {

        h.postDelayed(runnable, 1000); // 1 second delay (takes millis)

    }

    public void sendDataEventually(int i) {


        try {
            List<Integer> l = Arrays.asList(getRandomInRange(-180, 180, 6).intValue(),
                    getRandomInRange(-180, 180, 6).intValue());

            ParseObject tripStream = new ParseObject("StreamEvents");

            tripStream.put("TripID", tripID);
            tripStream.put("data", i);
//            tripStream.put("drivingHours", 0);
//            tripStream.put("elapsedTime", 0);
//            tripStream.put("g", "asdf");
//            tripStream.put("km", getRandomInRange(0, 3, 10));
//            tripStream.put("l", l);
//            tripStream.put("speed", Math.random());
//            tripStream.put("stopElapsedTime", 0);
//            tripStream.put("stops", Math.floor((Math.random() * 1) + 1));
//            tripStream.put("timestamp", System.currentTimeMillis());
//            tripStream.put("tripEvent", tevents[(int) Math.floor(Math.random() * tevents.length)]);

            tripStream.saveEventually(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e != null)
                        Log.e("ERR", e.getMessage(), e);
                    else {
                        reset();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void sendDataFromRabbitMQ() {

        j++;
        try {

            jobManager.addJobInBackground(new SendOfflineJob(j + ""));
            //new Thread(() -> new Send().send("{'data':'" + "hello" + "', 'type':'LIVE_DATA'}")).start();

            data.setText("Sending " + j);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private BigDecimal getRandomInRange(int from, int to, int fixed) {
        return new BigDecimal((Math.random() * (to - from) + from)).setScale(fixed * 1, RoundingMode.HALF_EVEN);
    }

    public void reset(View view) {

        if (h != null) {
            h.removeCallbacks(runnable);
        }

        tripID = "TP" + System.currentTimeMillis();
        reset();
    }

    public void reset() {
        try {
            counter = ParseQuery.getQuery("StreamEvents").fromLocalDatastore().count();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        data.setText("TripID:" + tripID + "\nLocations in queue : " + counter + "\n");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKFILE_REQUEST_CODE) {
            // Make sure the request was successful

            if (data != null) {


                try {
                    InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
                    final Bitmap selectedImage = BitmapFactory.decodeStream(inputStream);
                    Toast.makeText(MainActivity.this, "Uploading the file", Toast.LENGTH_SHORT).show();

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    selectedImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    selectedImage.recycle();

                    sendImageEventually(byteArray);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendImageEventually(byte[] byteArray) {
        final ParseFile file = new ParseFile(System.currentTimeMillis() + ".png", byteArray);
        reset();
        file.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                // Handle success or failure here ...

                ParseObject FileUpload = new ParseObject("FileUpload");
                FileUpload.put("Image", file);
                FileUpload.put("ImageURL", file.getUrl());
                FileUpload.put("TripID", tripID);
                FileUpload.saveEventually(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null)
                            Log.e("ERR", e.getMessage(), e);
                        else {
                            reset();
                        }
                    }
                });
            }
        }, new ProgressCallback() {
            public void done(Integer percentDone) {
                // Update your progress spinner here. percentDone will be between 0 and 100.
                data.setText("TripID:" + tripID + "\nLocations in queue : " + counter + "\n" + "Uploading " + percentDone);
            }
        });


    }


    public void pickFile(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }
}




