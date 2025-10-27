package com.example.server.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    boolean existsBySessionIdAndStudent_Id(Long sessionId, Long studentId);

    Optional<AttendanceRecord> findBySessionIdAndStudent_Id(Long sessionId, Long studentId);

    long countBySessionId(Long sessionId);

    List<AttendanceRecord> findAllBySessionId(Long sessionId);

    List<AttendanceRecord> findAllBySessionIdOrderByCheckedAtAsc(Long sessionId);

    List<AttendanceRecord> findAllBySessionIdInAndStudent_Id(List<Long> sessionIds, Long studentId);
}
