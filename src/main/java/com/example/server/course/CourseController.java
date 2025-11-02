package com.example.server.course;

import com.example.server.attendance.AttendanceRecordRepository;
import com.example.server.attendance.AttendanceSession;
import com.example.server.attendance.AttendanceSessionRepository;
import com.example.server.user.User;
import com.example.server.user.UserRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseRepository repo;
    private final UserRepository userRepo;
    private final AttendanceSessionRepository sessionRepo;
    private final AttendanceRecordRepository recordRepo;

    public CourseController(CourseRepository repo,
                            UserRepository userRepo,
                            AttendanceSessionRepository sessionRepo,
                            AttendanceRecordRepository recordRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.recordRepo = recordRepo;
    }

    @GetMapping
    public List<CourseDto> myCourses(@AuthenticationPrincipal UserDetails principal) {
        Long userName = parsePrincipal(principal);
        return repo.findAllByOwner_UserName(userName)
                .stream().map(CourseDto::from).toList();
    }

    @GetMapping("/my")
    public List<CourseDto> myCoursesAlt(@AuthenticationPrincipal UserDetails principal) {
        Long userName = parsePrincipal(principal);
        return repo.findAllByOwner_UserName(userName)
                .stream().map(CourseDto::from).toList();
    }

    @PostMapping
    public CourseDto create(@RequestBody @Valid CourseCreateRequest req,
                            @AuthenticationPrincipal UserDetails principal) {
        Long userName = parsePrincipal(principal);

        User owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (req.studentIds() == null || req.studentIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "En az bir öğrenci seçmelisiniz");
        }

        List<User> fetched = userRepo.findAllById(req.studentIds());
        if (fetched.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz öğrenci listesi");
        }

        Course c = new Course();
        c.setCourseName(req.courseName());
        c.setCourseCode(req.courseCode());
        c.setOwner(owner);

        Set<User> students = new HashSet<>(fetched);
        c.setStudents(students);

        return CourseDto.from(repo.save(c));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id,
                       @AuthenticationPrincipal UserDetails principal) {
        Long userName;
        try {
            userName = Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal");
        }
        var owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        var course = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (!course.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can delete the course");
        }

        var sessions = sessionRepo.findAllByCourse_IdOrderByCreatedAtDesc(id);

        if (!sessions.isEmpty()) {
            var sessionIds = sessions.stream().map(AttendanceSession::getId).toList();
            recordRepo.deleteAllBySessionIdIn(sessionIds);
            sessionRepo.deleteAll(sessions);
        }
        repo.deleteAllCourseStudents(id);
        repo.delete(course);
    }

    private Long parsePrincipal(UserDetails principal) {
        try {
            return Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal");
        }
    }
}
