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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    public AuthController(AuthenticationManager authManager,
                          JwtService jwtService,
                          UserRepository repo) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.repo = repo;
    }

    public record LoginRequest(Long userName, String password) {}

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal UserDetails user) {
        Long userName;
        try {
            userName = Long.parseLong(user.getUsername());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal");
        }

        User u = repo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

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

            // JWT üret
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
}
