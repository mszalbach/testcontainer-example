package com.github.mszalbach.testcontainer.example;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.apache.http.NoHttpResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.greaterThan;

@Testcontainers
class ToxicProxyIT {

    private static Network network = Network.newNetwork();

    @Container
    private static MockServerContainer mockServer = new MockServerContainer().withNetwork(network);

    @Container
    private ToxiproxyContainer toxiproxy = new ToxiproxyContainer("shopify/toxiproxy:2.1.4").withNetwork(network);

    private ToxiproxyContainer.ContainerProxy proxy;

    @BeforeEach
    void setup() {
        proxy = toxiproxy.getProxy(mockServer, MockServerContainer.PORT);
    }


    @Test
    void should_have_slow_response_when_done_through_toxic_proxy() throws IOException {
        proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 2_000);

        given().get(proxyUrl()).then().time(greaterThan(1_900L));
    }


    @Test
    void should_timeout_when_done_through_toxic_proxy() throws IOException {
        proxy.toxics().timeout("timeout", ToxicDirection.UPSTREAM, 100);

        assertThatExceptionOfType(NoHttpResponseException.class).isThrownBy(() -> given().get(proxyUrl()).then().statusCode(200));
    }

    private String proxyUrl() {
        return "http://" + proxy.getContainerIpAddress() + ":" + proxy.getProxyPort();
    }
}
