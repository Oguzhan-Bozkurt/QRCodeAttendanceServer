package com.example.server.course;

import jakarta.validation.constraints.NotBlank;

public record CourseCreateRequest(
        @NotBlank String courseName,
        @NotBlank String courseCode
) {}
