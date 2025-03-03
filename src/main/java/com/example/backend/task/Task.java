package com.example.backend.task;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "tasks")
@Data
@Builder
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

    @Override
    public int compareTo(Task other) {
        if(this.getOrder() > other.getOrder())
            return 1;
        else if (this.getOrder().equals(other.getOrder()))
            return 0 ;
        return -1 ;
    }
}
