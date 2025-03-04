package com.example.backend.task;

import com.example.backend.enums.OrderMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    final int MAX_NESTING_LEVEL = 5;

    public List<String> getSubTasksId(String taskId) throws NoSuchElementException {
        List<String> allSubTasksId = new ArrayList<>();
        fillSubTasksList(taskId, allSubTasksId);
        allSubTasksId.remove(0);
        return allSubTasksId;
    }

    private void fillSubTasksList(String id, List<String> allSubIdList) throws NoSuchElementException {
        Task task = getTask(id);
        allSubIdList.add(id);
        for (String subTaskId : task.getSubTasksId()) {
            fillSubTasksList(subTaskId, allSubIdList);
        }
    }

    public List<String> getParentTasksId(String taskId, List<String> allTasks) throws NoSuchElementException {
        List<String> allParentTasksId = new ArrayList<>();
        Task task = getTask(taskId);
        if (task.getParentId() != null) {
            allParentTasksId = fillParentTasksList(task.getId(), task.getParentId(), allParentTasksId, allTasks);
        }
        return allParentTasksId;
    }

    private List<String> fillParentTasksList(String subTaskId,
                                     String parentId,
                                     List<String> allParentTasksId,
                                     List<String> allTasks) throws NoSuchElementException {
        Task parentTask = getTask(parentId);
        Task subTask = getTask(subTaskId);

        List<String> subTasksId = parentTask.getSubTasksId();
        List<Task> tasks = getAllTasks(subTasksId);

        if (subTask.getOrder() - 1 <= tasks.size() - 1) {
            tasks.add(subTask.getOrder() - 1, subTask);
        }
        else {
            tasks.add(subTask);
        }

        for (Task t:tasks) {
            t.setOrder(tasks.indexOf(t) + 1);
        }

        subTasksId.add(subTaskId);
        parentTask.setSubTasksId(subTasksId);
        taskRepository.save(parentTask);
        taskRepository.saveAll(tasks);

        if (!allTasks.contains(parentId)) {
            allParentTasksId.add(parentId);

            if (parentTask.getParentId() != null) {
                return fillParentTasksList(parentTask.getId(), parentTask.getParentId(), allParentTasksId, allTasks);
            }
            else {
                return allParentTasksId;
            }
        }
        else {
            return allParentTasksId;
        }
    }

    public String addTask(String title,
                        String description,
                        String date,
                        List<String> tags,
                        String parentId,
                          Integer order) throws NoSuchElementException, IndexOutOfBoundsException {

        LocalDate localDate = LocalDate.parse(date);

        if (!Objects.equals(parentId, "")) {
            Task parentTask = getTask(parentId);

            if (parentTask.getNestingLevel() == MAX_NESTING_LEVEL) {
                throw new IndexOutOfBoundsException(
                        "Forbidden creating task located on more than " + MAX_NESTING_LEVEL + " nesting level"
                );
            }

            else {
                int nesting_level = parentTask.getNestingLevel() + 1;
                Task task = taskRepository.save(
                        Task.builder()
                                .title(title)
                                .description(description)
                                .date(localDate)
                                .tagsId(tags)
                                .parentId(parentId)
                                .order(order)
                                .nestingLevel(nesting_level)
                                .build()
                );

                List<String> subTasks = parentTask.getSubTasksId();
                subTasks.add(task.getId());
                parentTask.setSubTasksId(subTasks);
                taskRepository.save(parentTask);
                return task.getId();
            }
        }
        else {
            Task task = Task.builder()
                    .title(title)
                    .description(description)
                    .date(localDate)
                    .tagsId(tags)
                    .parentId(null)
                    .order(order)
                    .nestingLevel(0)
                    .build();
            taskRepository.save(task);
            return task.getId();
        }

    }

    public void updateTask(String id,
                           String title,
                           String description,
                           String date,
                           List<String> tags,
                           String parentId,
                           Integer order) throws NoSuchElementException {

        LocalDate localDate = LocalDate.parse(date);
        Task task = getTask(id);
        task.setTitle(title);
        task.setDescription(description);
        task.setDate(localDate);
        task.setParentId(parentId);
        task.setTagsId(tags);
        task.setOrder(order);
        taskRepository.save(task);
    }
    public void deleteTasks(List<String> tasksId) {
        tasksId.forEach(this::deleteTask);
    }

    public void deleteTask(String id) {
        Task task = getTask(id);
        // deleting all nested tasks
        taskRepository.deleteAllById(getSubTasksId(id));

        if (task.getParentId() != null) {
            String parentId = task.getParentId();
            Task parentTask = getTask(parentId);

            // removing taskId from parent
            List<String> parentSubTasksId = parentTask.getSubTasksId();
            parentSubTasksId.remove(task.getId());

            List<Task> parentSubTasks = getAllTasks(parentSubTasksId);

            for (Task t:parentSubTasks) {
                t.setOrder(parentSubTasks.indexOf(t) + 1);
            }
            taskRepository.saveAll(parentSubTasks);

            if (parentSubTasksId.isEmpty() & parentTask.getNestingLevel() > 0) {
                parentTask.setNestingLevel(parentTask.getNestingLevel() - 1);
            }

            parentTask.setSubTasksId(parentSubTasksId);

            taskRepository.save(parentTask);
        }

        taskRepository.deleteById(id);
    }

    public Task getTask(String id) throws NoSuchElementException {
        Optional<Task> optionalTask = taskRepository.findById(id);
        optionalTask.orElseThrow(() -> new NoSuchElementException("Task with id " + id + " not found"));
        return optionalTask.get();
    }

    public void deleteTag(String taskId, String tagId) {
        Task task = getTask(taskId);
        List<String> tagsId = task.getTagsId();
        int tagIndex = 0;
        boolean isFind = false;

        for (int i = 0; i < tagsId.size(); i++) {
            if (Objects.equals(tagsId.get(i), tagId)) {
                tagIndex = i;
                isFind = true;
            }
        }

        if (isFind) {
            tagsId.remove(tagIndex);
        }

        taskRepository.save(task);
    }

    public void saveTask(Task task) {
        taskRepository.save(task);
    }

    public List<Task> getAllTasks(List<String> tasksId) {
        List<Task> tasks =  new ArrayList<>();
        for (String taskId: tasksId) {
            Optional<Task> optionalTask = taskRepository.findById(taskId);
            optionalTask.orElseThrow(() -> new NoSuchElementException("Task with id " + taskId + " not found"));
            tasks.add(optionalTask.get());
        }
        return tasks;
    }

    public void changeOrderByTask(String taskId, List<Task> tasks, OrderMode mode) {
        Task currentTask = getTask(taskId);
        if (currentTask.getParentId() == null) {
            if (mode == OrderMode.INSERT) {
                tasks.add(currentTask.getOrder() - 1, currentTask);
                for (Task t: tasks) {
                    t.setOrder(tasks.indexOf(t) + 1);
                }
            }
            else {
                List<Task> tasksAfterCurrentTask = tasks.subList(tasks.indexOf(currentTask) + 1, tasks.size());
                for (Task t:tasksAfterCurrentTask) {
                    t.setOrder(t.getOrder() - 1);
                }
                tasks = tasksAfterCurrentTask;
            }
            taskRepository.saveAll(tasks);
        }
    }
}