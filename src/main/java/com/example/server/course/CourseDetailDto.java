package com.example.server.course;

import com.example.server.user.UserDto;

import java.util.List;
import java.util.stream.Collectors;

public class CourseDetailDto {
    private Long id;
    private String courseName;
    private String courseCode;
    private List<UserDto> students;

    public CourseDetailDto(Course c) {
        this.id = c.getId();
        this.courseName = c.getCourseName();
        this.courseCode = c.getCourseCode();
        this.students = c.getStudents().stream()
                .map(UserDto::from)
                .collect(Collectors.toList());
    }

    public Long getId() { return id; }
    public String getCourseName() { return courseName; }
    public String getCourseCode() { return courseCode; }
    public List<UserDto> getStudents() { return students; }
}
