package com.github.mszalbach.testcontainer.example;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;

@Testcontainers
class ApplicationIT {

    private static Network network = Network.newNetwork();

    @Container
    private RabbitMQContainer rabbitMQContainer = new RabbitMQContainer().withNetwork(network).withNetworkAliases("rabbit");

    @Container
    private AppContainer app = new AppContainer().withEnv("SPRING_RABBITMQ_HOST", "rabbit").dependsOn(rabbitMQContainer).withNetwork(network);


    @Test
    void should_be_reachable() {
        given().get(app.getURL("/hello")).then().statusCode(SC_OK).body(is("Hello"));
    }
}
