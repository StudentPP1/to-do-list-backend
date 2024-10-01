package com.example.backend.request;

import lombok.Data;

import java.util.List;

@Data
public class RequestTask {
    private String title;
    private String description;
    private String date;
    private List<String> tags;
    private String parentId;
    private Integer order;
}
