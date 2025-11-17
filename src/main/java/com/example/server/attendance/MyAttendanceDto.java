package com.example.server.attendance;

import java.time.Instant;

public record MyAttendanceDto(
        Long sessionId,
        Long courseId,
        String courseName,
        String courseCode,
        Instant checkedAt,
        String description
) {}
