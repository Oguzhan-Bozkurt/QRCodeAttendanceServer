package com.example.server.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {
    boolean existsByCourse_IdAndStudent_Id(Long courseId, Long studentId);
    List<CourseEnrollment> findAllByCourse_Id(Long courseId);
    void deleteByCourse_IdAndStudent_Id(Long courseId, Long studentId);
}
