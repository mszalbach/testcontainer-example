package com.github.mszalbach.testcontainer.example;

import lombok.var;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers
class MockServerIT {

    // will be shared between test methods
    @Container
    private static final MockServerContainer mockServer = new MockServerContainer();

    private MockServerClient mockServerClient;

    @BeforeEach
    void setup() {
        mockServerClient = new MockServerClient(mockServer.getContainerIpAddress(), mockServer.getServerPort());
    }

    @AfterEach
    void teardown() {
        mockServerClient.reset();
    }


    @Test
    void should_return_a_response_when_configured_via_mock_server_client() {
        mockServerClient.when(request().withPath("/")).respond(response().withStatusCode(200).withBody("Peter the person!"));

        var body = given().get(mockServer.getEndpoint()).then().extract().body().asString();

        assertThat(body).isEqualTo("Peter the person!");
    }

    @Test
    void should_return_503_when_server_error_is_set_via_mock_server_client() {
        mockServerClient.when(request().withPath("/")).respond(response().withStatusCode(SC_SERVICE_UNAVAILABLE));

        given().get(mockServer.getEndpoint()).then().statusCode(503);
    }

    @Test
    void should_be_able_to_retrive_recored_requests() {
        mockServerClient.when(request().withPath("/")).respond(response().withStatusCode(SC_OK));

        given().param("query", "4").get(mockServer.getEndpoint()).then().statusCode(200);

        var requests = mockServerClient.retrieveRecordedRequests(request().withPath("/"));

        assertThat(requests).hasSize(1).extracting(httpRequest -> httpRequest.getFirstQueryStringParameter("query")).contains("4");
    }
}
