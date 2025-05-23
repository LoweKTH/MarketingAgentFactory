package com.exjobb.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor 
public class TaskDto {
    private Long id;
    private String taskId;
    private String contentType;
    private String topic;
    private String platform;
    private String status;
    private String generatedContent;
    private LocalDateTime createdAt;
    private UserDto createdBy; // Reference to a simplified User DTO

    // Getters, Setters, Constructors
}