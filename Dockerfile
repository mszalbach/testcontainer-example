FROM openjdk:8-jre-alpine
RUN adduser -S java
USER java
WORKDIR /home/java
COPY  target/testcontainer-example-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
