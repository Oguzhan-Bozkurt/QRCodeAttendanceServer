package com.example.server.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import jakarta.transaction.Transactional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByOwner_UserName(Long userName);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM course_students WHERE course_id = :courseId", nativeQuery = true)
    void deleteAllCourseStudents(@Param("courseId") Long courseId);
}
