package com.sivsivsree.reatimeandroid;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeoutException;

public class Send implements Serializable {

    private String queue;
    private Channel channel;
    private Connection connection;

    Send() {

        this.queue = "offline.queue";
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("192.168.47.153");
            factory.setUsername("user");
            factory.setPassword("bitnami");
            connection = factory.newConnection();
            channel = connection.createChannel();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }


    public void publish(String message) {

        if (channel != null) {
            try {
                channel.queueDeclare(queue, true, false, false, null);
                channel.basicPublish("", queue, null, message.getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public void close(String message) {
        try {
            channel.close();
            connection.close();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}