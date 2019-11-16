package com.github.mszalbach.testcontainer.example;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class AppContainer extends GenericContainer<AppContainer> {

    AppContainer() {
        super(System.getProperty("image.name"));
        addExposedPorts(8080);
        waitingFor(Wait.forHttp("/actuator/health"));
    }

    private String getBaseURL() {
        return "http://" + this.getContainerIpAddress() + ":" + this.getMappedPort(8080);
    }

    String getURL(String path) {
        return getBaseURL() + path;
    }
}
