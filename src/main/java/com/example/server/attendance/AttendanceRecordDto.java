package com.example.server.attendance;

import java.time.Instant;

public record AttendanceRecordDto(
        Long id,
        Long sessionId,
        Long studentId,
        Instant scannedAt
) {
    public static AttendanceRecordDto from(AttendanceRecord r) {
        return new AttendanceRecordDto(
                r.getId(),
                r.getSessionId(),
                r.getStudent().getId(),
                r.getCheckedAt()
        );
    }
}
