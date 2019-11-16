FROM openjdk:13-jdk-alpine
ARG jar=target/testcontainer-example-*.jar
RUN adduser -S java
USER java
WORKDIR /home/java
COPY  $jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
