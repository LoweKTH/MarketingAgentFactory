package com.exjobb.backend.controller;

import com.exjobb.backend.dto.ApiResponse;
import com.exjobb.backend.dto.SaveContentRequest;
import com.exjobb.backend.dto.TaskDto;
import com.exjobb.backend.entity.Task;
import com.exjobb.backend.entity.User;
import com.exjobb.backend.repository.TaskRepository;
import com.exjobb.backend.service.TaskRouterService;
import com.exjobb.backend.service.UserService;
import com.exjobb.backend.utils.TaskMapper;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@CrossOrigin(origins = "http://localhost:5173") 
public class TaskController {

    private final TaskRepository taskRepository;
    private final TaskRouterService taskRouterService;
    private final UserService userService; // Assuming you have a UserService to get the current user

    /**
     * Retrieves all tasks from the database.
     * The logic for fetching and converting to DTOs is handled in the service layer.
     *
     * @return A list of all tasks as TaskDto objects.
     */
    @GetMapping("/getTasks")
    public ResponseEntity<List<TaskDto>> getTasks() {
        List<TaskDto> taskDtos = taskRouterService.getAllTasks(); // Delegate to service
        return ResponseEntity.ok(taskDtos);
    }
    /**
     * Retrieves a single task by its unique taskId (human-readable ID).
     * The logic for fetching and converting to DTO is handled in the service layer.
     *
     * @param taskId The unique task identifier string.
     * @return The TaskDto if found, or a 404 Not Found response if not found.
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable String taskId) {
        try {
            TaskDto taskDto = taskRouterService.getTaskByExternalId(taskId); // Delegate to service
            return ResponseEntity.ok(taskDto);
        } catch (IllegalArgumentException e) {
            // Catch the exception thrown by the service if the task is not found
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable String taskId) {
        try {
            taskRouterService.deleteTask(taskId); // Call your service method
            return ResponseEntity.noContent().build(); // 204 No Content for successful deletion
        } catch (Exception e) {
            // Handle exceptions, e.g., task not found
            return ResponseEntity.notFound().build(); // 404 Not Found if task doesn't exist
            // Or return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * Save the generated content to database
     * Called when user clicks "Save" button
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> saveContent(
            @RequestBody SaveContentRequest saveRequest) {

        log.info("Saving content to database");

        try {
            User dummyUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email("test@example.com")
                    .build();

            String taskId = taskRouterService.saveGeneratedContent(saveRequest, dummyUser);

            return ResponseEntity.ok(
                    ApiResponse.success(taskId, "Content saved successfully")
            );

        } catch (Exception e) {
            log.error("Content save failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Content save failed", e.getMessage()));
        }
    }

    

    // You might also want to add endpoints for:
    // - Filtering by platform: @GetMapping("/user/platform/{platform}")
    // - Updating task status: @PutMapping("/{taskId}/status")
    // - Deleting tasks: @DeleteMapping("/{taskId}")
}