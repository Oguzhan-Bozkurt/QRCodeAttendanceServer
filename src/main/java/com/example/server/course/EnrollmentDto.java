package com.example.server.course;

import java.util.List;

public class EnrollmentDto {
    public record AddStudentsRequest(List<Long> userNames) {}
    public record CourseStudentDto(Long userId, Long userName, String name, String surname) {}
}
