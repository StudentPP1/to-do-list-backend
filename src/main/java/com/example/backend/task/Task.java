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
    public String id;
    public String title;
    public String description;
    public LocalDate date;
    public List<String> tagsId = new ArrayList<>();
    public List<String> subTasksId = new ArrayList<>();
    public String parentId = null;
    public Integer order;
    public Integer nestingLevel = 0;

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
