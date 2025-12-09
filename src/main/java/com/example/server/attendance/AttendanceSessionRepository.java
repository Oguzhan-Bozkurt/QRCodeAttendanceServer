package com.example.server.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    Optional<AttendanceSession> findFirstByCourse_IdAndIsActiveTrue(Long courseId);
    Optional<AttendanceSession> findBySecretAndIsActiveTrue(String secret);
    Optional<AttendanceSession> findFirstByCourse_IdOrderByCreatedAtDesc(Long courseId);
    List<AttendanceSession> findAllByCourse_IdOrderByCreatedAtDesc(Long courseId);
    long countByCourse_Id(Long courseId);
}
