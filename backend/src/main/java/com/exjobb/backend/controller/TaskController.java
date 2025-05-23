package com.exjobb.backend.controller;

import com.exjobb.backend.dto.TaskDto;
import com.exjobb.backend.entity.Task;
import com.exjobb.backend.entity.User;
import com.exjobb.backend.repository.TaskRepository;
import com.exjobb.backend.service.UserService;
import com.exjobb.backend.utils.TaskMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks") // Base path for task-related endpoints
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") 
public class TaskController {

    private final TaskRepository taskRepository;
    private final UserService userService; // Assuming you have a UserService to get the current user

    /**
     * Retrieves all tasks for the currently authenticated user.
     * Tasks are ordered by creation date in descending order.
     *
     * @return A list of tasks for the user, or an empty list if no tasks are found.
     */
    @GetMapping("/getTasks")
    public ResponseEntity<List<TaskDto>> getTasks() {
        List<Task> tasks = taskRepository.findAll();

        // Correct way to call the static toDto method from TaskMapper
        List<TaskDto> taskDtos = tasks.stream()
            .map(TaskMapper::toDto) // Corrected method reference
            .collect(Collectors.toList());

        return ResponseEntity.ok(taskDtos);
    }
    /**
     * Retrieves a single task by its unique taskId (human-readable ID).
     *
     * @param taskId The unique task identifier string.
     * @return The task if found, or a 404 Not Found response.
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTaskById(@PathVariable String taskId) {
        Optional<Task> taskOptional = taskRepository.findByTaskId(taskId);
        return taskOptional.map(ResponseEntity::ok)
                           .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // You might also want to add endpoints for:
    // - Filtering by platform: @GetMapping("/user/platform/{platform}")
    // - Updating task status: @PutMapping("/{taskId}/status")
    // - Deleting tasks: @DeleteMapping("/{taskId}")
}