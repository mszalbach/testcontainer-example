package com.github.mszalbach.testcontainer.example;

import com.google.gson.Gson;
import lombok.var;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest //(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(initializers = SpringBootTestIT.PropertiesInitializer.class)
@Testcontainers
class SpringBootTestIT {

    @Autowired
    private MockMvc mockMvc;

    private static List<Book> messages = new CopyOnWriteArrayList<>();

    //use with WebEnvironment.RANDOM_PORT and rest lib of your choice
    //    @LocalServerPort
    //    int port;

    @Container
    private static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer()
            .withExchange("books", "topic")
            .withQueue("booksQueue")
            .withBinding("books", "booksQueue");

    @Container
    private static MockServerContainer mockServer = new MockServerContainer();

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
    void should_search_for_books() throws Exception {
        mockServerClient.when(request().withPath("/search.json")).respond(response().withStatusCode(200).withBody("{\"books\":\"Many books\"}"));

        mockMvc.perform(get("/books/search").param("query", "Lord of the Rings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("books").value("Many books"));

        mockServerClient.verify(request().withPath("/search.json").withQueryStringParameter("q", "Lord of the Rings"));
    }

    @Test
    void should_inform_others_when_a_book_is_added() throws Exception {
        var newBook = new Book.BookBuilder().isbn("1234").author("Carl Carlson").name("D'oh!").build();
        var newBookJson = new Gson().toJson(newBook);
        mockMvc.perform(post("/books").content(newBookJson).contentType(APPLICATION_JSON_VALUE)).andExpect(status().isAccepted());

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(messages).hasSize(1));

        assertThat(messages.get(0)).isEqualToComparingFieldByField(newBook);
    }

    @RabbitListener(queues = "booksQueue")
    void newBookListener(Book receivedBook) {
        messages.add(receivedBook);
    }

    static class PropertiesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(@NotNull ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "spring.rabbitmq.port=" + rabbitMQContainer.getMappedPort(5672),
                    "openlibrary.url=" + mockServer.getEndpoint())
                    .applyTo(context);
        }
    }
}
