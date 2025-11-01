package com.example.server.course;

public record CourseDto(
        Long id,
        String courseName,
        String courseCode,
        Long owner) {
    public static CourseDto from(Course c) {
        return new CourseDto(
                c.getId(),
                c.getCourseName(),
                c.getCourseCode(),
                c.getOwner() != null ? c.getOwner().getUserName() : null
        );
    }
}
