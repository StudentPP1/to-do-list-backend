package com.example.backend.user;

import com.example.backend.tag.Tag;
import com.example.backend.tag.TagService;
import com.example.backend.task.Task;
import com.example.backend.task.TaskService;
import com.example.backend.token.Token;
import com.example.backend.token.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TaskService taskService;
    private final TokenRepository tokenRepository;
    private final TagService tagService;

    public UserService(UserRepository userRepository,
                       TaskService taskService,
                       TokenRepository tokenRepository, TagService tagService) {
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.tokenRepository = tokenRepository;
        this.tagService = tagService;
    }

    private User getUserFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        jwt = authHeader.substring(7);
        Optional<Token> optionalToken = tokenRepository.findByToken(jwt);
        optionalToken.orElseThrow(() -> new NoSuchElementException("Token not found"));
        Token token = optionalToken.get();
        return token.getUser();
    }

    public void deleteUser(HttpServletRequest request) {
        User user = getUserFromRequest(request);

        List<String> tasksId = user.getTasksId();
        List<String> tagsId = user.getTagsId();

        for (String taskId: tasksId) {
            try {
                taskService.deleteTask(taskId);
            } catch (Exception ignored) {
            }
        }

        for (String tagId: tagsId) {
            tagService.deleteTag(tagId);
        }

        for (Token t: user.getTokens()) {
            tokenRepository.deleteById(t.getId());
        }

        userRepository.deleteById(user.getId());
    }

    public User getUser(HttpServletRequest request) {
        return getUserFromRequest(request);
    }

    public void addTask(HttpServletRequest request,
                        String title,
                        String description,
                        String date,
                        List<String> tags,
                        String parentId,
                        Integer order) throws NoSuchElementException {
        User user = getUserFromRequest(request);

        List<String> tasksId = user.getTasksId();
        String taskId = taskService.addTask(title, description, date, tags, parentId, order);
        tasksId.add(taskId);
        user.setTasksId(tasksId);
        System.out.println("addTask: " + user.getTasksId());
        userRepository.save(user);
    }

    public void deleteTask(String taskId, HttpServletRequest request, String date) {
        System.out.println("deleted: " + taskId);
        User user = getUserFromRequest(request);

        List<String> dates = new ArrayList<>();
        dates.add(date);
        List<Task> tasks = getTasksByDate(request, dates).get(0);
        if (!tasks.isEmpty()) {
            taskService.changeOrderByTask(taskId, tasks, OrderMode.DELETE);
        }

        List<String> tasksId = taskService.getSubTasksId(taskId);
        tasksId.add(taskId);

        List<String> userTasksId = user.getTasksId();
        List<String> userDoneTasksId = user.getDoneTasksId();

        deleteTasksFromArray(tasksId, userTasksId);
        deleteTasksFromArray(tasksId, userDoneTasksId);

        user.setTasksId(userTasksId);
        user.setDoneTasksId(userDoneTasksId);
        try {
            taskService.deleteTask(taskId);
        } catch (Exception ignored) {

        }
        userRepository.save(user);
        System.out.println("New user tasks: " + user.getTasksId());
    }

    public List<Task> getAllTask(HttpServletRequest request) {
        List<Task> tasks = new ArrayList<>();
        User user = getUserFromRequest(request);
        List<String> tasksId = user.getTasksId();

        for (String taskId: tasksId) {
            tasks.add(taskService.getTask(taskId));
        }

        return tasks;
    }

    public List<Tag> getAllTag(HttpServletRequest request) {
        List<Tag> tags = new ArrayList<>();
        User user = getUserFromRequest(request);
        List<String> tagsId = user.getTagsId();

        for (String tagId: tagsId) {
            tags.add(tagService.getTag(tagId));
        }

        return tags;
    }

    public void deleteTag(String tagId, HttpServletRequest request) {
        User user = getUserFromRequest(request);
        List<String> tagsId = user.getTagsId();
        int tagIndex = 0;

        for (int i = 0; i < tagsId.size(); i++) {
            if (Objects.equals(tagsId.get(i), tagId)) {
                tagIndex = i;
            }
        }
        tagsId.remove(tagIndex);
        user.setTagsId(tagsId);

        for (String taskId: user.getTasksId()) {
            taskService.deleteTag(taskId, tagId);
        }

        tagService.deleteTag(tagId);
        userRepository.save(user);
    }

    public void addTag(HttpServletRequest request, String name, String color) {
        User user = getUserFromRequest(request);
        String tagId = tagService.addTag(user.getId(), name, color);
        List<String> tagsId = user.getTagsId();
        tagsId.add(tagId);
        userRepository.save(user);
    }

    public List<Task> getTasksByTag(String tagId, HttpServletRequest request) {
        List<Task> tasks = new ArrayList<>();
        User user = getUserFromRequest(request);
        for (String taskId: user.getTasksId()) {

            Task task = taskService.getTask(taskId);
            for (String taskTagId: task.getTagsId()) {
                if (Objects.equals(taskTagId, tagId)) {
                    tasks.add(task);
                }
            }
        }

        return tasks;
    }

    public List<List<Task>> getTasksByDate(HttpServletRequest request, List<String> dates) {
        List<List<Task>> taskByDates = new ArrayList<>();

        User user = getUserFromRequest(request);
        System.out.println("User tasks: " + user.getTasksId());
        for (String date: dates) {
            List<Task> tasks = new ArrayList<>();
            for (String taskId: user.getTasksId()) {
                Task task = taskService.getTask(taskId);

                if (task.getDate().toString().equals(date)) {
                    if (task.getParentId() == null) {
                        tasks.add(task);
                    }
                }
            }
            tasks.sort(Comparator.comparingInt(Task::getOrder));
            taskByDates.add(tasks);
        }
        System.out.println("getTasksByDate: " + taskByDates);
        return taskByDates;
    }

    public void doneTask(String taskId, HttpServletRequest request, String date) {
        User user = getUserFromRequest(request);

        List<Task> tasks = new ArrayList<>();
        for (String id: user.getTasksId()) {
            Task task = taskService.getTask(id);

            if (task.getDate().toString().equals(date)) {
                if (task.getParentId() == null) {
                    tasks.add(task);
                }
            }
        }
        tasks.sort(Comparator.comparingInt(Task::getOrder));

        if (!tasks.isEmpty()) {
            taskService.changeOrderByTask(taskId, tasks, OrderMode.DELETE);
        }

        List<String> tasksId = taskService.getSubTasksId(taskId);
        tasksId.add(taskId);

        List<String> userTasksId = user.getTasksId();
        deleteTasksFromArray(tasksId, userTasksId);
        List<String> userDoneTasksId = user.getDoneTasksId();

        userDoneTasksId.addAll(tasksId);

        user.setDoneTasksId(userDoneTasksId);
        user.setTasksId(userTasksId);
        userRepository.save(user);

        Task task = taskService.getTask(taskId);
        if (task.getParentId() != null) {
            String parentId = task.getParentId();
            Task parentTask = taskService.getTask(parentId);

            // removing taskId from parent
            List<String> parentSubTasksId = parentTask.getSubTasksId();
            parentSubTasksId.remove(task.getId());

            if (parentSubTasksId.isEmpty() & parentTask.getNestingLevel() > 0) {
                parentTask.setNestingLevel(parentTask.getNestingLevel() - 1);
            }

            parentTask.setSubTasksId(parentSubTasksId);

            taskService.saveTask(parentTask);
        }
    }

    public List<Task> getOverdueTasks(HttpServletRequest request, String date) {
        LocalDate overdueDate = LocalDate.parse(date);
        List<Task> tasks = new ArrayList<>();
        User user = getUserFromRequest(request);

        for (String taskId: user.getTasksId()) {
            Task task = taskService.getTask(taskId);
            if (task.getDate().isBefore(overdueDate) && task.getParentId() == null) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    public Map<String, List<Task>> getDoneTasks(List<String> dates, HttpServletRequest request) {
        Map<String, List<Task>> map = new HashMap<>();

        User user = getUserFromRequest(request);
        for (String date: dates) {
            List<Task> tasks = new ArrayList<>();

            for (String taskId: user.getDoneTasksId()) {
                try {
                    Task task = taskService.getTask(taskId);

                    if (task.getParentId() == null && task.getDate().toString().equals(date)) {
                        tasks.add(task);
                    }
                } catch (Exception ignored) {
                }
            }

            map.put(date, tasks);
        }
        System.out.println(map);
        return map;
    }

    public void replaceTaskToActive(String taskId, String date, HttpServletRequest request) {
        User user = getUserFromRequest(request);
        List<String> dates = new ArrayList<>();
        dates.add(date);
        List<Task> tasks = getTasksByDate(request, dates).get(0);
        if (!tasks.isEmpty()) {
            taskService.changeOrderByTask(taskId, tasks, OrderMode.INSERT);
        }

        List<String> userActiveTasksId = user.getTasksId();
        List<String> userDoneTasksId = user.getDoneTasksId();

        List<String> tasksId = taskService.getSubTasksId(taskId);
        tasksId.add(taskId);

        deleteTasksFromArray(tasksId, userDoneTasksId);
        userActiveTasksId.addAll(tasksId);

        user.setDoneTasksId(userDoneTasksId);
        user.setTasksId(userActiveTasksId);

        userRepository.save(user);
    }

    private void deleteTasksFromArray(List<String> tasksId, List<String> userDoneTasksId) {
        for (String id: tasksId) {
            int taskIndex = -1;

            for (int i = 0; i < userDoneTasksId.size(); i++) {
                if (Objects.equals(userDoneTasksId.get(i), id)) {
                    taskIndex = i;
                }
            }

            if (taskIndex != -1) {
                userDoneTasksId.remove(taskIndex);
            }
        }
    }
}
