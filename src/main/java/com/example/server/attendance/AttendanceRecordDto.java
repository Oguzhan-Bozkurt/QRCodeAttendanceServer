package com.example.server.attendance;

import com.example.server.user.User;

import java.time.Instant;

public record AttendanceRecordDto(
        Long id,
        Long studentId,
        Long userName,
        String name,
        String surname,
        Instant checkedAt
) {
    public static AttendanceRecordDto from(AttendanceRecord r) {
        User s = r.getStudent();
        return new AttendanceRecordDto(
                r.getId(),
                s.getId(),
                s.getUserName(),
                s.getName(),
                s.getSurname(),
                r.getCheckedAt()
        );
    }
}
