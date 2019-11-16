package com.github.mszalbach.testcontainer.example;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "isbn")
class Book {
    String isbn;
    String name;
    String author;
}
