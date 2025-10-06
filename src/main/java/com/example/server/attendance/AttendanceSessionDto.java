package com.example.server.attendance;

import java.time.Instant;

public record AttendanceSessionDto(
        Long id,
        Long courseId,
        String secret,
        Instant expiresAt,
        boolean active,
        Instant createdAt
) {
    public static AttendanceSessionDto from(AttendanceSession s) {
        return new AttendanceSessionDto(
                s.getId(),
                s.getCourse().getId(),
                s.getSecret(),
                s.getExpiresAt(),
                s.isActive(),
                s.getCreatedAt()
        );
    }
}
