package com.example.backend.user;

import com.example.backend.enums.OrderMode;
import com.example.backend.jwt.service.JwtService;
import com.example.backend.request.RequestTask;
import com.example.backend.tag.Tag;
import com.example.backend.tag.TagService;
import com.example.backend.task.Task;
import com.example.backend.task.TaskService;
import com.example.backend.token.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final TaskService taskService;
    private final TagService tagService;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public User loadUserByUsername(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("user not found")
        );
    }

    public void resetPassword(User user, String newPassword) {
        user.setPassword(newPassword);
        userRepository.save(user);
    }

    public void deleteUser() {
        User user = getUser();
        try {
            taskService.deleteTasks(user.getTasksId());
        } catch (Exception ignored) {
        }
        tagService.deleteTags(user.getTagsId());
        tokenService.deleteTokens(user.getTokens());
        userRepository.deleteById(user.getId());
    }

    public void addTask(RequestTask task)  {
        User user = getUser();
        List<String> tasksId = user.getTasksId();
        String taskId = taskService.addTask(
                task.getTitle(),
                task.getDescription(),
                task.getDate(),
                task.getTags(),
                task.getParentId(),
                task.getOrder());
        tasksId.add(taskId);
        user.setTasksId(tasksId);
        userRepository.save(user);
    }

    public void deleteTask(String taskId, String date) {
        User user = getUser();

        List<String> dates = new ArrayList<>();
        dates.add(date);
        List<Task> tasks = getTasksByDate(dates).get(0);
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
    }

    public List<Task> getAllTask() {
        return getUser().getTasksId().stream().map(taskService::getTask).toList();
    }

    public List<Tag> getAllTag() {
        return getUser().getTagsId().stream().map(tagService::getTag).toList();
    }

    public void deleteTag(String tagId) {
        User user = getUser();
        List<String> tagsId = user.getTagsId();
        int tagIndex = 0;

        for (int i = 0; i < tagsId.size(); i++) {
            if (Objects.equals(tagsId.get(i), tagId)) {
                tagIndex = i;
            }
        }
        tagsId.remove(tagIndex);
        user.setTagsId(tagsId);

        user.getTasksId().forEach(taskId -> taskService.deleteTag(taskId, tagId));

        tagService.deleteTag(tagId);
        userRepository.save(user);
    }

    public void addTag(String name, String color)  {
        User user = getUser();
        String tagId = tagService.addTag(user.getId(), name, color);
        List<String> tagsId = user.getTagsId();
        tagsId.add(tagId);
        userRepository.save(user);
    }

    public List<Task> getTasksByTag(String tagId) {
        return getUser().getTasksId().stream()
                .map(taskService::getTask)
                .filter(task -> task.getTagsId().contains(tagId))
                .toList();
    }

    public List<List<Task>> getTasksByDate(List<String> dates) {
        List<List<Task>> taskByDates = new ArrayList<>();
        for (String date: dates) {
            List<Task> tasks = getTasksByDate(date, getUser());
            taskByDates.add(tasks);
        }
        return taskByDates;
    }

    public void doneTask(String taskId, String date) {
        User user = getUser();

        List<Task> tasks = getTasksByDate(date, user);
        if (!tasks.isEmpty()) {
            taskService.changeOrderByTask(taskId, tasks, OrderMode.DELETE);
        }

        // done task and it's sub tasks
        List<String> tasksId = taskService.getSubTasksId(taskId);
        tasksId.add(taskId);

        for (Task t: taskService.getAllTasks(tasksId)) {
            t.setSubTasksId(new ArrayList<>());
            // set done date as today & save current date
            t.setDateDone(t.getDate());
            t.setDate(LocalDate.now());
            taskService.saveTask(t);
        }

        List<String> userTasksId = user.getTasksId();
        deleteTasksFromArray(tasksId, userTasksId);
        List<String> userDoneTasksId = user.getDoneTasksId();
        userDoneTasksId.addAll(tasksId);

        user.setDoneTasksId(userDoneTasksId);
        user.setTasksId(userTasksId);
        userRepository.save(user);

        Task task = taskService.getTask(taskId);

        // update parent task (if present)
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

    public List<Task> getOverdueTasks(String date) {
        LocalDate overdueDate = LocalDate.parse(date);
        return getUser().getTasksId().stream()
                .map(taskService::getTask)
                .filter(task -> task.getDate().isBefore(overdueDate) && task.getParentId() == null)
                .toList();
    }

    public Map<String, List<Task>> getDoneTasks(List<String> dates) {
        return getUser().getDoneTasksId().stream()
                .map(taskService::getTask)
                .filter(task -> dates.contains(task.getDate().toString()))
                .collect(Collectors.groupingBy(task -> task.getDate().toString(), Collectors.toList()));
    }

    public void replaceTaskToActive(String taskId, String date) {
        User user = getUser();

        Task task = taskService.getTask(taskId);
        List<String> userActiveTasksId = user.getTasksId();
        List<String> userDoneTasksId = user.getDoneTasksId();
        List<String> tasksId;

        if (task.getParentId() != null) {
            tasksId = taskService.getParentTasksId(task.getId(), userActiveTasksId);
        } else {
            List<String> dates = new ArrayList<>();
            dates.add(date);
            List<Task> tasks = getTasksByDate(dates).get(0);
            if (!tasks.isEmpty()) {
                taskService.changeOrderByTask(taskId, tasks, OrderMode.INSERT);
            }
            tasksId = taskService.getSubTasksId(taskId);
        }

        tasksId.add(taskId);

        for (Task t: taskService.getAllTasks(tasksId)) {
            t.setDate(t.getDateDone());
            t.setDateDone(null);
            taskService.saveTask(t);
        }

        userDoneTasksId.removeAll(tasksId);
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
    private List<Task> getTasksByDate(String date, User user) {
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
        return tasks;
    }

    public User getUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails user) {
            String email = jwtService.extractEmail(user.getUsername());
            return getUserByEmail(email);
        }
        else {
            throw new UsernameNotFoundException("user not found");
        }
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user not found"));
    }

    public User getUserById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("user not found"));
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }
}
