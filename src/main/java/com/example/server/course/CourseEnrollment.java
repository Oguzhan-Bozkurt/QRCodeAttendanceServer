package com.example.server.course;

import com.example.server.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "course_students",
        uniqueConstraints = @UniqueConstraint(name = "uq_course_student", columnNames = {"course_id","student_id"}))
public class CourseEnrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(optional = false) @JoinColumn(name = "student_id")
    private com.example.server.user.User student;

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
}
