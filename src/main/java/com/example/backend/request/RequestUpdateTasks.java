package com.example.backend.request;

import lombok.Data;

import java.util.List;

@Data
public class RequestUpdateTasks {
    private List<RequestUpdateTask> tasks;
}
