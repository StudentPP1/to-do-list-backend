package com.example.backend.user;

import com.example.backend.request.RequestDate;
import com.example.backend.tag.Tag;
import com.example.backend.request.RequestTask;
import com.example.backend.task.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @GetMapping("/getUser")
    public User getUser() {
        return userService.getUser();
    }

    @DeleteMapping("/deleteUser")
    public void deleteUser() {
        userService.deleteUser();
    }

    @PostMapping("/addTask")
    public void addTask(@RequestBody RequestTask task) {
        userService.addTask(task);
    }

    @GetMapping("/getTasks")
    public List<Task> getTasks() {
        return userService.getAllTask();
    }

    @DeleteMapping("/deleteTask")
    public void deleteTask(
            @RequestParam(name = "taskId") String taskId,
            @RequestParam(name = "date") String date
    ) {
        userService.deleteTask(taskId, date);
    }

    @PostMapping("/addTag")
    public void addTag(@RequestBody Map<String, String> request) {
        userService.addTag(request.get("name"), request.get("color"));
    }

    @GetMapping("/getTags")
    public List<Tag> getTags() { return userService.getAllTag(); }

    @DeleteMapping("/deleteTag")
    public void deleteTag(@RequestParam(name = "tagId") String tagId) {
        userService.deleteTag(tagId);
    }

    @GetMapping("/getTasksByTag")
    public List<Task> getTasksByTag(@RequestParam(name = "tagId") String tagId)  {
        return userService.getTasksByTag(tagId);
    }

    @PostMapping("/getTasksByDate")
    public List<List<Task>> getTasksByDate(@RequestBody(required = false) RequestDate date) {
        return userService.getTasksByDate(date.getDates());
    }

    @GetMapping("/getOverdueTasks")
    public List<Task> getOverdueTasks(@RequestParam(name = "date") String date) {
        return userService.getOverdueTasks(date);
    }

    @PostMapping("/doneTask")
    public void doneTask(
            @RequestParam(name = "taskId") String taskId,
            @RequestParam(name = "date") String date
    ) {
        userService.doneTask(taskId, date);
    }

    @PostMapping("/getDoneTasks")
    public Map<String, List<Task>> getDoneTasks(@RequestBody(required = false) RequestDate date) {
        return userService.getDoneTasks(date.getDates());
    }

    @PostMapping("/replaceTaskToActive")
    public void replaceTaskToActive(@RequestParam(name = "taskId") String taskId,
                                    @RequestParam(name = "date") String date
    ) {
        userService.replaceTaskToActive(taskId, date);
    }

}
