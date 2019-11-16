package com.github.mszalbach.testcontainer.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("books")
@Slf4j
public class BookController {

    private RabbitTemplate rabbitTemplate;

    private RestTemplate restTemplate;

    @Value("${openlibrary.url:http://openlibrary.org}")
    private String openlibraryUrl;

    public BookController(RabbitTemplate rabbitTemplate, RestTemplateBuilder restTemplateBuilder) {
        this.rabbitTemplate = rabbitTemplate;
        this.restTemplate = restTemplateBuilder.build();
    }

    @GetMapping(value = "/search", produces = APPLICATION_JSON_VALUE)
    public String bookSearch(@RequestParam String query) {
        return restTemplate.getForObject(openlibraryUrl + "/search.json?q=" + query, String.class);
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity addBook(@RequestBody Book book) {
        //do something with book
        // inform everyone about new book
        rabbitTemplate.convertAndSend("books", "", book);
        return ResponseEntity.accepted().build();
    }
}
