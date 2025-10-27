package com.example.server.auth;

import com.example.server.security.JwtService;
import com.example.server.user.User;
import com.example.server.user.UserDto;
import com.example.server.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepository repo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authManager,
                          JwtService jwtService,
                          UserRepository repo,
                          PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public record LoginRequest(Long userName, String password) {}
    public record RegisterRequest(Long userName, String password, String name, String surName, boolean userIsStudent, String title) {}

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal UserDetails user) {
        Long userName;
        try {
            userName = Long.parseLong(user.getUsername());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz kimlik");
        }

        User u = repo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        return UserDto.from(u);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            var token = new UsernamePasswordAuthenticationToken(
                    req.userName().toString(), req.password()
            );
            Authentication auth = authManager.authenticate(token);
            var principal = (UserDetails) auth.getPrincipal();

            String accessToken = jwtService.generateToken(principal);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "user", principal.getUsername(),
                    "accessToken", accessToken,
                    "ts", Instant.now().toString()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "message", "Geçersiz kullanıcı adı veya şifre"
            ));
        }
    }

    @PostMapping("/register")
    public UserDto register(@RequestBody RegisterRequest req) {
        if (req.userName() == null || req.password() == null || req.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kullanıcı adı ve şifre gerekli");
        }

        boolean exists = repo.findByUserName(req.userName()).isPresent();
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu kullanıcı zaten mevcut");
        }

        User u = new User();
        u.setUserName(req.userName());
        u.setName(req.name() != null ? req.name().trim() : null);
        u.setSurname(req.surName() != null ? req.surName().trim() : null);
        u.setPassword(passwordEncoder.encode(req.password()));
        boolean userIsStudent = req.userIsStudent();
        u.setUserIsStudent(userIsStudent);
        u.setTitle(req.title() != null ? req.title().trim() : null);

        User saved = repo.save(u);
        return UserDto.from(saved);
    }
}
