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

    // --------- DTO'lar ----------
    public record StartRequest(Integer minutes) {}
    public record CheckinRequest(String secret) {}
    public record CheckinResponse(Long sessionId, Instant checkedAt, String status) {}

    // --------- Yardımcılar ----------
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

    // --------- Oturum Başlat ----------
    @PostMapping("/start")
    public AttendanceSessionDto start(@PathVariable Long courseId,
                                      @RequestBody(required = false) StartRequest req,
                                      @AuthenticationPrincipal UserDetails principal) {
        User owner = currentUser(principal);
        Course course = ownedCourseOr403(courseId, owner);

        // aktif varsa kapat
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
        Long userName;
        try {
            userName = Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal");
        }

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

    // --------- Oturumu Bitir (SADECE SAHİBİ) ----------
    @PostMapping("/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(@PathVariable Long courseId,
                     @AuthenticationPrincipal UserDetails principal) {
        // 1) Kullanıcıyı bul
        Long userName;
        try {
            userName = Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal");
        }

        User owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // 2) Kursu ve aktif oturumu bul
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can stop attendance");
        }

        var activeOpt = sessionRepo.findFirstByCourse_IdAndIsActiveTrue(courseId);
        var active = activeOpt.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No active session"));

        // 3) Bitir
        active.setActive(false);
        sessionRepo.save(active);
    }

    // --------- Check-in (ÖĞRENCİ) ----------
    // Not: Bu uç, URL'de courseId bekler. QR içinde courseId ve secret'ı göndereceğiz.
    @PostMapping("/checkin")
    public CheckinResponse checkin(@PathVariable Long courseId,
                                   @RequestBody CheckinRequest req,
                                   @AuthenticationPrincipal UserDetails principal) {
        if (req == null || req.secret() == null || req.secret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "secret is required");
        }

        User student = currentUser(principal);

        // Aktif oturumu secret ile bul
        AttendanceSession session = sessionRepo.findBySecretAndIsActiveTrue(req.secret())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found or inactive"));

        // courseId uyuşsun
        if (!session.getCourse().getId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course mismatch");
        }

        // Süresi geçmiş mi?
        if (session.getExpiresAt() != null && Instant.now().isAfter(session.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
        }

        // Sahibi kendisi olamaz
        if (session.getOwner().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner cannot check-in");
        }

        // Çift kayıt engelle
        if (recordRepo.existsBySessionIdAndStudent_Id(session.getId(), student.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already checked-in");
        }

        // Kaydet
        AttendanceRecord rec = new AttendanceRecord();
        rec.setSessionId(session.getId());
        rec.setStudent(student);
        rec.setCheckedAt(Instant.now());
        recordRepo.save(rec);

        return new CheckinResponse(session.getId(), rec.getCheckedAt(), "ok");
    }

    // --------- Secret üretimi ----------
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RAND = new SecureRandom();

    private String generateSecret(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(ALPHANUM.charAt(RAND.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
