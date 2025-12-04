package com.example.server.attendance;

import java.util.List;

public class ManualAttendanceRequest {
    private List<Long> studentIds;

    public ManualAttendanceRequest() {}

    public ManualAttendanceRequest(List<Long> studentIds) {
        this.studentIds = studentIds;
    }

    public List<Long> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<Long> studentIds) {
        this.studentIds = studentIds;
    }
}
