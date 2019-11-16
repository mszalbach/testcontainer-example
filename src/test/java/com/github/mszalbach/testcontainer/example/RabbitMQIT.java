package com.github.mszalbach.testcontainer.example;

import com.rabbitmq.client.ConnectionFactory;
import lombok.var;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RabbitMQIT {

    // will be started before and stopped after each test method
    @Container
    private RabbitMQContainer rabbitMQContainer = new RabbitMQContainer()
            .withExchange("userEvents", "topic")
            .withQueue("userTestQueue")
            .withBinding("userEvents", "userTestQueue");

    @Test
    @Timeout(value = 30)
    void should_be_able_to_send_and_receive_a_message_from_RabbitMQ() throws Exception {

        var completableFuture = new CompletableFuture<String>();
        var connectionFactory = new ConnectionFactory();
        connectionFactory.setUri(rabbitMQContainer.getAmqpUrl());
        connectionFactory.setUsername(rabbitMQContainer.getAdminUsername());
        connectionFactory.setPassword(rabbitMQContainer.getAdminPassword());

        connectionFactory.newConnection().createChannel().basicConsume("userTestQueue",
                (consumerTag, delivery) -> completableFuture.complete(new String(delivery.getBody())),
                (consumerTag, delivery) -> {
                    completableFuture.cancel(true);
                });

        connectionFactory.newConnection().createChannel().basicPublish("userEvents", "", null, "Hallo".getBytes());

        var message = completableFuture.get();
        assertThat(message).isEqualTo("Hallo");
    }

}
