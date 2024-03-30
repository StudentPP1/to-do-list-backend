package com.example.backend.task;

import lombok.Data;

import java.util.List;

@Data
public class RequestUpdateTasks {
    List<RequestUpdateTask> tasks;
}
