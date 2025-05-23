package com.exjobb.backend.utils;

import com.exjobb.backend.dto.TaskDto;
import com.exjobb.backend.dto.UserDto;
import com.exjobb.backend.entity.Task;
import com.exjobb.backend.entity.User;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for converting between Task entities and TaskDto Data Transfer Objects.
 */
public class TaskMapper {

    // Private constructor to prevent instantiation for utility class
    private TaskMapper() {
        // Utility class
    }

    /**
     * Converts a Task entity to a TaskDto.
     *
     * @param task The Task entity to convert.
     * @return The corresponding TaskDto.
     */
    public static TaskDto toDto(Task task) {
        if (task == null) {
            return null;
        }

        TaskDto dto = new TaskDto(); // Explicitly instantiate
        dto.setId(task.getId());
        dto.setTaskId(task.getTaskId());
        dto.setContentType(task.getContentType());
        dto.setTopic(task.getTopic());
        dto.setPlatform(task.getPlatform());
        dto.setGeneratedContent(task.getGeneratedContent());
        dto.setCreatedAt(task.getCreatedAt());


        if (task.getCreatedBy() != null) {
            UserDto userDto = new UserDto(); // Explicitly instantiate
            userDto.setId(task.getCreatedBy().getId());
            userDto.setUsername(task.getCreatedBy().getUsername());
            userDto.setFullName(task.getCreatedBy().getFullName());
            dto.setCreatedBy(userDto);
        }

        return dto;
    }

    /**
     * Converts a list of Task entities to a list of TaskDtos.
     *
     * @param tasks The list of Task entities to convert.
     * @return A list of corresponding TaskDtos.
     */
    public static List<TaskDto> toDtoList(List<Task> tasks) {
        if (tasks == null) {
            return List.of(); // Return an empty list instead of null
        }
        return tasks.stream()
                .map(TaskMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Converts a TaskDto to a Task entity.
     * This method is typically used when receiving data from the frontend
     * to create or update a Task entity in the database.
     *
     * @param taskDto The TaskDto to convert.
     * @param createdBy The User entity who created or is associated with this task.
     * This is typically fetched from the database based on authentication.
     * @return The corresponding Task entity.
     */
    public static Task toEntity(TaskDto taskDto, User createdBy) {
        if (taskDto == null) {
            return null;
        }

        Task task = new Task(); // Explicitly instantiate
        // Note: ID is usually set by the database on creation, or for updates.
        // task.setId(taskDto.getId()); // Uncomment if you're mapping an ID for updates
        task.setTaskId(taskDto.getTaskId()); // Important for existing tasks
        task.setContentType(taskDto.getContentType());
        task.setTopic(taskDto.getTopic());
        task.setPlatform(taskDto.getPlatform());


        task.setGeneratedContent(taskDto.getGeneratedContent());

        task.setCreatedBy(createdBy); // Set the actual User entity
        // CreatedAt and UpdatedAt are usually handled by @CreatedDate/@LastModifiedDate
        // task.setCreatedAt(taskDto.getCreatedAt());
        // task.setUpdatedAt(taskDto.getUpdatedAt());

        return task;
    }
}