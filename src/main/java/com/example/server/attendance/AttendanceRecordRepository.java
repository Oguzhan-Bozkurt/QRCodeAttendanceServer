package com.example.server.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    boolean existsBySessionIdAndStudent_Id(Long sessionId, Long studentId);

    Optional<AttendanceRecord> findBySessionIdAndStudent_Id(Long sessionId, Long studentId);

    long countBySessionId(Long sessionId);

    List<AttendanceRecord> findAllBySessionId(Long sessionId);

    List<AttendanceRecord> findAllBySessionIdOrderByCheckedAtAsc(Long sessionId);

    List<AttendanceRecord> findAllBySessionIdInAndStudent_Id(List<Long> sessionIds, Long studentId);

    void deleteAllBySessionIdIn(List<Long> sessionIds);

    @Query("""
       select new com.example.server.attendance.MyAttendanceDto(
           s.id,
           c.id,
           c.courseName,
           c.courseCode,
           r.checkedAt,
           s.description
       )
       from AttendanceRecord r
       join AttendanceSession s on s.id = r.sessionId
       join Course c on c.id = s.course.id
       where r.student.id = :studentId
       order by r.checkedAt desc
       """)
    List<MyAttendanceDto> findMyAttendance(@Param("studentId") Long studentId);
}
