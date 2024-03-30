package com.example.backend.tag;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "tags")
public class Tag {
    @Id
    String id;
    String name;
    String color;
    String userId;

    public Tag(String name, String color, String userId) {
        this.name = name;
        this.color = color;
        this.userId = userId;
    }
}
