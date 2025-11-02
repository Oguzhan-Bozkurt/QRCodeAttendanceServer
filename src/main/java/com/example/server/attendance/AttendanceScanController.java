package com.example.server.attendance;

import com.example.server.user.User;
import com.example.server.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/attendance")
public class AttendanceScanController {

    private final AttendanceSessionRepository sessionRepo;
    private final AttendanceRecordRepository recordRepo;
    private final UserRepository userRepo;

    public AttendanceScanController(AttendanceSessionRepository sessionRepo,
                                    AttendanceRecordRepository recordRepo,
                                    UserRepository userRepo) {
        this.sessionRepo = sessionRepo;
        this.recordRepo = recordRepo;
        this.userRepo = userRepo;
    }

    public record ScanRequest(String secret) {}

    @PostMapping("/scan")
    public AttendanceRecordDto scan(@RequestBody ScanRequest req,
                                    @AuthenticationPrincipal UserDetails principal) {
        if (req == null || req.secret() == null || req.secret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Secret boş olamaz");
        }

        Long userName;
        try {
            userName = Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal");
        }
        User student = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        AttendanceSession s = sessionRepo.findBySecretAndIsActiveTrue(req.secret())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Geçersiz veya kapalı oturum"));

        if (Instant.now().isAfter(s.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Oturum süresi doldu");
        }

        var existing = recordRepo.findBySessionIdAndStudent_Id(s.getId(), student.getId());
        if (existing.isPresent()) {
            return AttendanceRecordDto.from(existing.get());
        }

        AttendanceRecord r = new AttendanceRecord();
        r.setSessionId(s.getId());
        r.setStudent(student);
        r.setCheckedAt(Instant.now());

        return AttendanceRecordDto.from(recordRepo.save(r));
    }
}
