package com.github.mszalbach.testcontainer.example;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "isbn")
class Book {
    private String isbn;
    private String name;
    private String author;
}
