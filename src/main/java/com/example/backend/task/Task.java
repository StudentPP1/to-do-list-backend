package com.example.backend.task;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "tasks")
@Data
public class Task implements Comparable<Task>{

    @Id
    private String id;
    private String title;
    private String description;
    private LocalDate date;
    private List<String> tagsId;
    private List<String> subTasksId = new ArrayList<>();
    private String parentId;
    private Integer order;
    private Integer nestingLevel;
    private LocalDate dateDone = null;

    public Task(
            String title,
            String description,
            LocalDate date,
            List<String> tagsId,
            String parentId,
            Integer order,
            Integer nestingLevel)
    {
        this.title = title;
        this.description = description;
        this.date = date;
        this.tagsId = tagsId;
        this.parentId = parentId;
        this.order = order;
        this.nestingLevel = nestingLevel;
    }

    @Override
    public int compareTo(Task other) {
        if(this.getOrder() > other.getOrder())
            return 1;
        else if (this.getOrder().equals(other.getOrder()))
            return 0 ;
        return -1 ;
    }
}
