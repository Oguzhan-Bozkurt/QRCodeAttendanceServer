package com.example.server.attendance;

import com.example.server.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/attendance")
public class AttendanceMyController {

    private final UserRepository userRepo;
    private final AttendanceRecordRepository recordRepo;

    public AttendanceMyController(UserRepository userRepo,
                                  AttendanceRecordRepository recordRepo) {
        this.userRepo = userRepo;
        this.recordRepo = recordRepo;
    }

    @GetMapping("/my")
    public List<MyAttendanceDto> myAttendance(@AuthenticationPrincipal UserDetails principal) {
        Long userName;
        try {
            userName = Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz kimlik");
        }

        var user = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kullanıcı bulunamadı"));

        return recordRepo.findMyAttendance(user.getId());
    }
}
