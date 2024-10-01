package com.example.backend.request;

import lombok.Data;

import java.util.List;

@Data
public class RequestUpdateTasks {
    List<RequestUpdateTask> tasks;
}
