package com.example.server.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository repo;
    private final UserService userService;

    @GetMapping
    public List<UserDto> all() {
        return repo.findAll().stream().map(UserDto::from).toList();
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody @Valid UserCreateRequest req) {
        User saved = userService.create(req);
        URI location = URI.create("/users/" + saved.getId());
        return ResponseEntity.created(location).body(UserDto.from(saved));
    }
}
