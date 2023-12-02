package org.test.app;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.TimeoutException;


public class rev {
    private final static String QUEUE_NAME = "Test_Rabbit_TimeOut";

    public static void main(String[] argv){
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = null;
        try {
            connection = factory.newConnection();

            Channel channel = connection.createChannel();

            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                System.out.println("[START] at  " + LocalDateTime.now());
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Received '" + message + "'");
                SimpleClientHttpRequestFactory reqFactory = new SimpleClientHttpRequestFactory();
                reqFactory.setReadTimeout(40000); //closing the connection before rabbitTimeout -> message will be in Unack will try again
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.setRequestFactory(reqFactory);

                // Define the URL you want to make a request to
                String apiUrl = "http://localhost:9090/admin/downloadFile?id=3"; // this endpoint waits for 2 min and consumer timeout is 1

                // Make a GET request and retrieve the response
                ResponseEntity<String> responseEntity = restTemplate.getForEntity(apiUrl, String.class);

                // Extract and print the response body
                String responseBody = responseEntity.getBody();
                System.out.println("Response Body: " + responseBody);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                System.out.println("[ENDS] at  " + LocalDateTime.now());
            };
            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
            });
        }catch (Exception e) {
            System.out.println(e);
        }
        //https://www.rabbitmq.com/consumers.html#acknowledgement-timeout
        // https://www.rabbitmq.com/tutorials/tutorial-two-java.html => under Message acknowledgment
    }

}

/**
 rabbitmqctl set_policy queue_consumer_timeout "with_delivery_timeout\.*" '{"consumer-timeout":60000}' --apply-to Test_Rabbit_TimeOut
 rabbitmqctl set_policy queue_consumer_timeout "Test_Rabbit_TimeOut" '{"consumer-timeout": 60000}' --apply-to queues
 */

/*
ConnectionTimeout. This is timeout in millis for establishing connection between source and destination

ReadTimeout. This is timeout in millis which expects the response/result should be returned from the destination endpoint.
*/
