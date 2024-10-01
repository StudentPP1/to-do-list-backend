package com.example.backend.task;
import com.example.backend.request.RequestTask;
import com.example.backend.request.RequestTasksId;
import com.example.backend.request.RequestUpdateTask;
import com.example.backend.request.RequestUpdateTasks;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/get")
    public Task getTask(@RequestParam(name = "taskId") String taskId) {
        return taskService.getTask(taskId);
    }

    @PostMapping("/update")
    public void updateTask(@RequestParam(name = "taskId") String taskId, @RequestBody RequestTask task) {
        taskService.updateTask(
                taskId,
                task.getTitle(),
                task.getDescription(),
                task.getDate(),
                task.getTags(),
                task.getParentId(),
                task.getOrder()
        );
    }

    @PostMapping("/updateSome")
    public void updateSomeTask(@RequestBody RequestUpdateTasks requestUpdateTasks) {
        for (RequestUpdateTask task: requestUpdateTasks.getTasks()) {
            System.out.println(task);
            taskService.updateTask(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getDate(),
                    task.getTags(),
                    task.getParentId(),
                    task.getOrder()
            );
        }
    }

    @PostMapping("/getAll")
    public List<Task> getAllTasks(@RequestBody(required = false) RequestTasksId tasksId) {
        return taskService.getAllTasks(tasksId.getTasks());
    }
}
