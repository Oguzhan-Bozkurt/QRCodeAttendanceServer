package com.example.server.attendance;

import com.example.server.course.Course;
import com.example.server.course.CourseRepository;
import com.example.server.user.User;
import com.example.server.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/courses/{courseId}/attendance")
public class AttendanceController {

    private final CourseRepository courseRepo;
    private final UserRepository userRepo;
    private final AttendanceSessionRepository sessionRepo;
    private final AttendanceRecordRepository recordRepo;

    public AttendanceController(CourseRepository courseRepo,
                                UserRepository userRepo,
                                AttendanceSessionRepository sessionRepo,
                                AttendanceRecordRepository recordRepo) {
        this.courseRepo = courseRepo;
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.recordRepo = recordRepo;
    }

    public record StartRequest(Integer minutes) {}
    public record CheckinRequest(String secret) {}
    public record CheckinResponse(Long sessionId, Instant checkedAt, String status) {}
    public record ActiveSummaryDto(Long sessionId, Instant expiresAt, long count, boolean active) {}

    private Long principalUserName(UserDetails principal) {
        try {
            return Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal");
        }
    }

    private User currentUser(UserDetails principal) {
        Long userName = principalUserName(principal);
        return userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Course ownedCourseOr403(Long courseId, User owner) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (!c.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can manage attendance");
        }
        return c;
    }

    @GetMapping("/active/summary")
    public ActiveSummaryDto activeSummary(@PathVariable Long courseId,
                                          @AuthenticationPrincipal UserDetails principal) {
        Long userName = principalUserName(principal);
        User owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (!course.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can view");
        }

        AttendanceSession s = sessionRepo.findFirstByCourse_IdAndIsActiveTrue(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active session"));

        long cnt = recordRepo.countBySessionId(s.getId());
        return new ActiveSummaryDto(s.getId(), s.getExpiresAt(), cnt, s.isActive());
    }

    @PostMapping("/start")
    public AttendanceSessionDto start(@PathVariable Long courseId,
                                      @RequestBody(required = false) StartRequest req,
                                      @AuthenticationPrincipal UserDetails principal) {
        User owner = currentUser(principal);
        Course course = ownedCourseOr403(courseId, owner);

        sessionRepo.findFirstByCourse_IdAndIsActiveTrue(courseId).ifPresent(s -> {
            s.setActive(false);
            sessionRepo.save(s);
        });

        int mins = (req != null && req.minutes() != null) ? req.minutes() : 10;
        if (mins < 1) mins = 1;
        if (mins > 180) mins = 180;

        AttendanceSession s = new AttendanceSession();
        s.setCourse(course);
        s.setOwner(owner);
        s.setSecret(generateSecret(16));
        s.setExpiresAt(Instant.now().plus(Duration.ofMinutes(mins)));
        s.setActive(true);

        return AttendanceSessionDto.from(sessionRepo.save(s));
    }

    @GetMapping("/active")
    public AttendanceSessionDto active(@PathVariable Long courseId,
                                       @AuthenticationPrincipal UserDetails principal) {
        Long userName = principalUserName(principal);

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ders bulunamadı"));

        User owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kullanıcı bulunamadı"));

        if (!course.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Yalnızca sahibi aktif oturumu görüntüleyebilir");
        }

        return sessionRepo.findFirstByCourse_IdAndIsActiveTrue(courseId)
                .map(AttendanceSessionDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aktif oturum yok"));
    }

    @PostMapping("/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(@PathVariable Long courseId,
                     @AuthenticationPrincipal UserDetails principal) {
        Long userName = principalUserName(principal);
        User owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can stop attendance");
        }

        var active = sessionRepo.findFirstByCourse_IdAndIsActiveTrue(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active session"));

        active.setActive(false);
        sessionRepo.save(active);
    }

    @PostMapping("/checkin")
    public CheckinResponse checkin(@PathVariable Long courseId,
                                   @RequestBody CheckinRequest req,
                                   @AuthenticationPrincipal UserDetails principal) {
        if (req == null || req.secret() == null || req.secret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "secret is required");
        }
        final String secret = req.secret().trim();

        User student = currentUser(principal);

        AttendanceSession session = sessionRepo.findBySecretAndIsActiveTrue(secret)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found or inactive"));

        if (!session.getCourse().getId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course mismatch");
        }

        if (session.getExpiresAt() != null && Instant.now().isAfter(session.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
        }

        if (session.getOwner().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner cannot check-in");
        }

        if (recordRepo.existsBySessionIdAndStudent_Id(session.getId(), student.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already checked-in");
        }

        AttendanceRecord rec = new AttendanceRecord();
        rec.setSessionId(session.getId());
        rec.setStudent(student);
        rec.setCheckedAt(Instant.now());
        recordRepo.save(rec);

        return new CheckinResponse(session.getId(), rec.getCheckedAt(), "ok");
    }

    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RAND = new SecureRandom();

    private String generateSecret(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(ALPHANUM.charAt(RAND.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

    @GetMapping("/active/records")
    public java.util.List<AttendanceRecordDto> activeRecords(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long userName = principalUserName(principal);
        var owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        var course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (!course.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can view attendees");
        }

        var active = sessionRepo.findFirstByCourse_IdAndIsActiveTrue(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active session"));

        var records = recordRepo.findAllBySessionIdOrderByCheckedAtAsc(active.getId());
        return records.stream().map(AttendanceRecordDto::from).toList();
    }
    public record SessionWithCountDto(
            Long id,
            Instant createdAt,
            Instant expiresAt,
            boolean active,
            long count
    ) {
        public static SessionWithCountDto of(AttendanceSession s, long count) {
            return new SessionWithCountDto(
                    s.getId(),
                    s.getCreatedAt(),
                    s.getExpiresAt(),
                    s.isActive(),
                    count
            );
        }
    }
    @GetMapping("/history")
    public java.util.List<SessionWithCountDto> history(@PathVariable Long courseId,
                                                       @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Long userName;
        try {
            userName = Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid principal");
        }
        var owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));

        var course = courseRepo.findById(courseId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Course not found"));
        if (!course.getOwner().getId().equals(owner.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Only owner can view history");
        }

        var sessions = sessionRepo.findAllByCourse_IdOrderByCreatedAtDesc(courseId);
        return sessions.stream()
                .map(s -> SessionWithCountDto.of(s, recordRepo.countBySessionId(s.getId())))
                .toList();
    }

    @GetMapping("/{sessionId}")
    public AttendanceSessionDto sessionDetail(@PathVariable Long courseId,
                                              @PathVariable Long sessionId,
                                              @AuthenticationPrincipal UserDetails principal) {
        Long userName;
        try { userName = Long.parseLong(principal.getUsername()); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal"); }

        User owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (!course.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can view");
        }

        AttendanceSession s = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!s.getCourse().getId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course mismatch");
        }

        return AttendanceSessionDto.from(s);
    }

    @GetMapping("/{sessionId}/records")
    public java.util.List<AttendanceRecordDto> sessionRecords(@PathVariable Long courseId,
                                                              @PathVariable Long sessionId,
                                                              @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Long userName;
        try { userName = Long.parseLong(principal.getUsername()); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal"); }

        User owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (!course.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can view");
        }

        AttendanceSession s = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!s.getCourse().getId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course mismatch");
        }

        var records = recordRepo.findAllBySessionIdOrderByCheckedAtAsc(sessionId);
        return records.stream().map(AttendanceRecordDto::from).toList();
    }
}