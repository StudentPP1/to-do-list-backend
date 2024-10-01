package com.example.backend.user;

import com.example.backend.request.RequestDate;
import com.example.backend.tag.Tag;
import com.example.backend.request.RequestTask;
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
    public User getUser(HttpServletRequest request) throws Exception {
        return userService.getUser(request);
    }

    @DeleteMapping("/deleteUser")
    public void deleteUser(HttpServletRequest request) throws Exception {
        userService.deleteUser(request);
    }

    @PostMapping("/addTask")
    public void addTask(HttpServletRequest request, @RequestBody RequestTask task) throws Exception {
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
    public List<Task> getTasks(HttpServletRequest request) throws Exception {
        return userService.getAllTask(request);
    }

    @DeleteMapping("/deleteTask")
    public void deleteTask(@RequestParam(name = "taskId") String taskId, @RequestParam(name = "date") String date,
                           HttpServletRequest request) throws Exception {
        userService.deleteTask(taskId, request, date);
    }

    @PostMapping("/addTag")
    public void addTag(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) throws Exception {
        userService.addTag(httpRequest, request.get("name"), request.get("color"));
    }

    @GetMapping("/getTags")
    public List<Tag> getTags(HttpServletRequest request) throws Exception { return userService.getAllTag(request); }

    @DeleteMapping("/deleteTag")
    public void deleteTag(@RequestParam(name = "tagId") String tagId, HttpServletRequest request) throws Exception {
        userService.deleteTag(tagId, request);
    }

    @GetMapping("/getTasksByTag")
    public List<Task> getTasksByTag(@RequestParam(name = "tagId") String tagId, HttpServletRequest request) throws Exception {
        return userService.getTasksByTag(tagId, request);
    }

    @PostMapping("/getTasksByDate")
    public List<List<Task>> getTasksByDate(@RequestBody(required = false) RequestDate date, HttpServletRequest request) throws Exception {
        return userService.getTasksByDate(request, date.getDates());
    }

    @GetMapping("/getOverdueTasks")
    public List<Task> getOverdueTasks(@RequestParam(name = "date") String date, HttpServletRequest request) throws Exception {
        return userService.getOverdueTasks(request, date);
    }

    @PostMapping("/doneTask")
    public void doneTask(@RequestParam(name = "taskId") String taskId, @RequestParam(name = "date") String date,
                         HttpServletRequest request) throws Exception {
        userService.doneTask(taskId, request, date);
    }

    @PostMapping("/getDoneTasks")
    public Map<String, List<Task>> getDoneTasks(@RequestBody(required = false) RequestDate date, HttpServletRequest request) throws Exception {
        return userService.getDoneTasks(date.getDates(), request);
    }

    @PostMapping("/replaceTaskToActive")
    public void replaceTaskToActive(@RequestParam(name = "taskId") String taskId,
                                    @RequestParam(name = "date") String date,
                                    HttpServletRequest request) throws Exception {
        userService.replaceTaskToActive(taskId, date, request);
    }

}
