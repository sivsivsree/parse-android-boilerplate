package com.sivsivsree.reatimeandroid;

import android.util.Log;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.Serializable;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class Xsend implements Serializable {

    private Thread publishThread;

    private BlockingDeque<String> queue = new LinkedBlockingDeque<String>();
    private ConnectionFactory factory = new ConnectionFactory();
    String topic = "";

    public Xsend(String topic) {
        this.topic = topic;
        setupConnectionFactory();
        publishToAMQP();
    }

    public void publishMessage(String message) {
        try {
            queue.putLast(message);
            //Log.d("XSEND", "[q] " + message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void setupConnectionFactory() {
        try {
            factory.setAutomaticRecoveryEnabled(false);
            factory.setHost("localhost");
            factory.setUsername("user");
            factory.setPassword("bitnami");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void publishToAMQP() {
        publishThread = new Thread(() -> {
            while (true) {
                try {
                    Connection connection = factory.newConnection();
                    Channel ch = connection.createChannel();
                    ch.confirmSelect();
                    while (true) {
                        String message = queue.takeFirst();
                        try {
                            ch.exchangeDeclare("LIVEONE", "topic");
                            ch.basicPublish("LIVEONE", topic, null, message.getBytes());
                            Log.d("XSEND", "[s] " + message);
                            ch.waitForConfirmsOrDie();
                        } catch (Exception e) {
                            Log.d("XSEND", "[f] " + message);
                            queue.putFirst(message);
                            throw e;
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {

                    Log.d("XSEND", queue.size() + " Connection broken: " + e.getClass().getName());
                    e.printStackTrace();
                    try {
                        Thread.sleep(500); //sleep and then try again
                    } catch (InterruptedException e1) {
                        break;
                    }
                }
            }
        });
        publishThread.start();
    }



    public void stop(){
        publishThread.interrupt();
    }

}