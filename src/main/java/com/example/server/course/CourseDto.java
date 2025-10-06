package com.example.server.course;

public record CourseDto(
        Long id,
        String courseName,
        String courseCode
) {
    public static CourseDto from(Course c) {
        return new CourseDto(c.getId(), c.getCourseName(), c.getCourseCode());
    }
}
