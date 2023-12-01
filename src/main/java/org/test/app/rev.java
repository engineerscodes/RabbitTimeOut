package org.test.app;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;


public class rev {
    private final static String QUEUE_NAME = "Test_Rabbit_TimeOut";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            System.out.println("[START] at  "+LocalDateTime.now());
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
            try {
                Thread.sleep(120000); // wait for 2 min
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            System.out.println("[ENDS] at  "+ LocalDateTime.now());
        };
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
        //https://www.rabbitmq.com/consumers.html#acknowledgement-timeout
        // https://www.rabbitmq.com/tutorials/tutorial-two-java.html => under Message acknowledgment
    }

}

/**
 rabbitmqctl set_policy queue_consumer_timeout "with_delivery_timeout\.*" '{"consumer-timeout":60000}' --apply-to Test_Rabbit_TimeOut
 rabbitmqctl set_policy queue_consumer_timeout "Test_Rabbit_TimeOut" '{"consumer-timeout": 60000}' --apply-to queues
 */