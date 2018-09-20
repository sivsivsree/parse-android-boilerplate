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


    Send() {

        this.queue = "data_queue";

    }


    public void send(String message) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("192.168.1.105");
            factory.setUsername("root");
            factory.setPassword("root");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(queue, true, false, false, null);
            channel.basicPublish("", queue, null, message.getBytes("UTF-8"));
            channel.close();
            connection.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}

