package com.example.server.course;

import com.example.server.user.User;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="course_name", nullable=false)
    private String courseName;

    @Column(name="course_code", nullable=false)
    private String courseCode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private com.example.server.user.User owner;

    @ManyToMany
    @JoinTable(
            name = "course_students",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"),
            uniqueConstraints = @UniqueConstraint(
                    name="uq_course_student", columnNames = {"course_id","student_id"}
            )
    )
    private Set<User> students = new HashSet<>();

    public Long getId() { return id; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public com.example.server.user.User getOwner() { return owner; }
    public void setOwner(com.example.server.user.User owner) { this.owner = owner; }

    public Set<User> getStudents() { return students; }
    public void setStudents(Set<User> students) { this.students = students; }
}
