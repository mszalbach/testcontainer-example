package com.github.mszalbach.testcontainer.example;

import lombok.var;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers
class MockServerIT {

    // will be shared between test methods
    @Container
    private static MockServerContainer mockServer = new MockServerContainer();

    private static MockServerClient mockServerClient;

    @BeforeAll
    static void setup() {
        mockServerClient = new MockServerClient(mockServer.getContainerIpAddress(), mockServer.getServerPort());
    }

    @AfterEach
    void teardown() {
        mockServerClient.reset();
    }


    @Test
    void should_return_a_response_when_configured_via_mock_server_client() {
        mockServerClient.when(request().withPath("/")).respond(response().withStatusCode(SC_OK).withBody("Peter the person!"));

        given().get(mockServer.getEndpoint()).then().body(is("Peter the person!"));
    }

    @Test
    void should_return_503_when_server_error_is_set_via_mock_server_client() {
        mockServerClient.when(request().withPath("/")).respond(response().withStatusCode(SC_SERVICE_UNAVAILABLE));

        given().get(mockServer.getEndpoint()).then().statusCode(SC_SERVICE_UNAVAILABLE);
    }

    @Test
    void should_be_able_to_retrieve_recorded_requests() {
        mockServerClient.when(request().withPath("/")).respond(response().withStatusCode(SC_OK));

        given().param("query", "4").get(mockServer.getEndpoint()).then().statusCode(SC_OK);

        var requests = mockServerClient.retrieveRecordedRequests(request().withPath("/"));

        assertThat(requests).hasSize(1).extracting(httpRequest -> httpRequest.getFirstQueryStringParameter("query")).contains("4");
    }
}
