package com.example.server.attendance;

import com.example.server.course.Course;
import com.example.server.course.CourseRepository;
import com.example.server.user.User;
import com.example.server.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attendance")
public class AttendanceMyController {

    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final AttendanceSessionRepository sessionRepo;
    private final AttendanceRecordRepository recordRepo;

    public AttendanceMyController(UserRepository userRepo,
                                  CourseRepository courseRepo,
                                  AttendanceSessionRepository sessionRepo,
                                  AttendanceRecordRepository recordRepo) {
        this.userRepo = userRepo;
        this.courseRepo = courseRepo;
        this.sessionRepo = sessionRepo;
        this.recordRepo = recordRepo;
    }

    public record MyAttendanceSummaryDto(
            long courseId,
            String courseName,
            String courseCode,
            int totalSessions,
            int attendedSessions
    ) {}

    @GetMapping("/my")
    public List<MyAttendanceSummaryDto> getMyAttendanceSummary(@AuthenticationPrincipal UserDetails principal) {
        Long userId;
        try {
            userId = Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Geçersiz kullanıcı kimliği");
        }

        User student = userRepo.findByUserName(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kullanıcı bulunamadı"));

        if (!student.isUserIsStudent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu işlem yalnızca öğrenciler içindir");
        }

        List<Course> enrolledCourses = courseRepo.findAllByStudents_Id(student.getId());

        return enrolledCourses.stream()
                .map(course -> {
                    long totalSessions = sessionRepo.countByCourse_Id(course.getId());
                    long attendedSessions = recordRepo.countAttendedSessionsByStudentAndCourse(student.getId(), course.getId());

                    return new MyAttendanceSummaryDto(
                            course.getId(),
                            course.getCourseName(),
                            course.getCourseCode(),
                            (int) totalSessions,
                            (int) attendedSessions
                    );
                })
                .collect(Collectors.toList());
    }
}