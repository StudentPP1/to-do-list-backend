package com.example.backend.task;

import lombok.Data;

import java.util.List;
@Data
public class RequestUpdateTask {
    String id;
    String title;
    String description;
    String date;
    List<String> tags;
    String parentId;
    Integer order;
}
