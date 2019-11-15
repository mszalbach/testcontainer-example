FROM openjdk:13-jdk-alpine
ARG jar=target/testcontainer-example-0.0.1-SNAPSHOT.jar
RUN adduser -S java
USER java
ADD  $jar app.jar
EXPOSE 8080
CMD java -jar app.jar