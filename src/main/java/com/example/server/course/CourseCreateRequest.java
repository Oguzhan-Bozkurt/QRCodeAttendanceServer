package com.example.server.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CourseCreateRequest(
        @NotBlank String courseName,
        @NotBlank String courseCode,
        @NotNull List<Long> studentIds
) { public String getCourseName() { return courseName; }
    public String getCourseCode() { return courseCode; }
    public List<Long> getStudentIds() { return studentIds; }}
