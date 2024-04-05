package com.example.backend.user;

import com.example.backend.tag.Tag;
import com.example.backend.task.RequestTask;
import com.example.backend.task.Task;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/getUser")
    public User getUser(HttpServletRequest request) {
        return userService.getUser(request);
    }

    @DeleteMapping("/deleteUser")
    public void deleteUser(HttpServletRequest request) {
        userService.deleteUser(request);
    }

    @PostMapping("/addTask")
    public void addTask(HttpServletRequest request, @RequestBody RequestTask task) {
        userService.addTask(
                request,
                task.getTitle(),
                task.getDescription(),
                task.getDate(),
                task.getTags(),
                task.getParentId(),
                task.getOrder());
    }

    @GetMapping("/getTasks")
    public List<Task> getTasks(HttpServletRequest request) {
        return userService.getAllTask(request);
    }

    @DeleteMapping("/deleteTask")
    public void deleteTask(@RequestParam(name = "taskId") String taskId, HttpServletRequest request) {
        userService.deleteTask(taskId, request);
    }

    @PostMapping("/addTag")
    public void addTag(@RequestBody Map<String, String> request, HttpServletRequest http_request) {
        userService.addTag(http_request, request.get("name"), request.get("color"));
    }

    @GetMapping("/getTags")
    public List<Tag> getTags(HttpServletRequest request) { return userService.getAllTag(request); }

    @DeleteMapping("/deleteTag")
    public void deleteTag(@RequestParam(name = "tagId") String tagId, HttpServletRequest request) {
        userService.deleteTag(tagId, request);
    }

    @GetMapping("/getTasksByTag")
    public List<Task> getTasksByTag(@RequestParam(name = "tagId") String tagId, HttpServletRequest request) {
        return userService.getTasksByTag(tagId, request);
    }

    @PostMapping("/getTasksByDate")
    public List<List<Task>> getTasksByDate(@RequestBody(required = false) RequestDate date, HttpServletRequest request) {
        return userService.getTasksByDate(request, date.getDates());
    }

    @GetMapping("/getOverdueTasks")
    public List<Task> getOverdueTasks(@RequestParam(name = "date") String date, HttpServletRequest request) {
        return userService.getOverdueTasks(request, date);
    }

    @PostMapping("/doneTask")
    public void doneTask(@RequestParam(name = "taskId") String taskId, HttpServletRequest request) {
        userService.doneTask(taskId, request);
    }

    @PostMapping("/getDoneTasks")
    public Map<String, List<Task>> getDoneTasks(@RequestBody(required = false) RequestDate date, HttpServletRequest request) {
        return userService.getDoneTasks(date.getDates(), request);
    }

    @PostMapping("/replaceTaskToActive")
    public void replaceTaskToActive(@RequestParam String taskId, HttpServletRequest request) {
        userService.replaceTaskToActive(taskId, request);
    }

}
