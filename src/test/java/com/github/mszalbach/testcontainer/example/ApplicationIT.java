package com.github.mszalbach.testcontainer.example;

import com.google.gson.Gson;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers
@Slf4j
class ApplicationIT {

    private static Network network = Network.newNetwork();

    @Container
    private static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer().withNetwork(network).withNetworkAliases("rabbit")
            .withExchange("books", "topic")
            .withQueue("booksQueue")
            .withBinding("books", "booksQueue");

    @Container
    private static MockServerContainer mockServer = new MockServerContainer().withNetworkAliases("openlibrary").withNetwork(network);

    @Container
    private static AppContainer app = new AppContainer()
            .withEnv("SPRING_RABBITMQ_HOST", "rabbit")
            .withEnv("openlibrary_url", "http://openlibrary:1080")
            .dependsOn(rabbitMQContainer, mockServer)
            .withNetwork(network)
            .withLogConsumer(new Slf4jLogConsumer(log));

    private static MockServerClient mockServerClient;

    @BeforeAll
    static void setup() {
        mockServerClient = new MockServerClient(mockServer.getContainerIpAddress(), mockServer.getServerPort());
    }

    @AfterAll
    static void teardown() {
        mockServerClient.reset();
    }

    @Test
    void should_search_for_books() {
        mockServerClient.when(request().withPath("/search.json")).respond(response().withStatusCode(200).withBody("{\"books\":\"Many books\"}"));

        given().param("query", "Lord of the Rings").get(app.getURL("/books/search"))
                .then().statusCode(SC_OK).body("books", is("Many books"));

        mockServerClient.verify(request().withPath("/search.json").withQueryStringParameter("q", "Lord of the Rings"));
    }

    @Test
    void should_inform_others_when_a_book_is_added() throws Exception {
        var newBook = new Book.BookBuilder().isbn("1234").author("Carl Carlson").name("D'oh!").build();

        var messageFuture = subscribe();
        given().body(newBook).contentType(JSON).post(app.getURL("/books")).then().statusCode(SC_ACCEPTED);

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(messageFuture).isCompleted());

        var sendBook = new Gson().fromJson(messageFuture.get(), Book.class);
        assertThat(sendBook).isEqualToComparingFieldByField(newBook);
    }

    private CompletableFuture<String> subscribe() throws Exception {
        var completableFuture = new CompletableFuture<String>();
        var connectionFactory = new ConnectionFactory();
        connectionFactory.setUri(rabbitMQContainer.getAmqpUrl());
        connectionFactory.setUsername(rabbitMQContainer.getAdminUsername());
        connectionFactory.setPassword(rabbitMQContainer.getAdminPassword());

        connectionFactory.newConnection().createChannel().basicConsume("booksQueue",
                (consumerTag, delivery) -> completableFuture.complete(new String(delivery.getBody())),
                (consumerTag, delivery) -> {
                    completableFuture.cancel(true);
                });

        return completableFuture;
    }
}
