package com.github.mszalbach.testcontainer.example;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class AppContainer extends GenericContainer<AppContainer> {

    private static final int HTTP_PORT = 8080;

    AppContainer() {
        super(System.getProperty("image.name"));
        addExposedPorts(HTTP_PORT);
        waitingFor(Wait.forHttp("/actuator/health"));
    }

    private String getBaseURL() {
        return String.format("http://%s:%d", this.getContainerIpAddress(), this.getMappedPort(HTTP_PORT));
    }

    String getURL(String path) {
        return getBaseURL() + path;
    }
}
