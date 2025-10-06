package com.example.server.user;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User create(UserCreateRequest req) {
        if (repo.existsByUserName(req.userName())) {
            throw new DuplicateResourceException(req.userName());
        }
        User u = new User();
        u.setUserName(req.userName());
        u.setName(req.name());
        u.setSurname(req.surname());
        u.setUserIsStudent(req.userIsStudent() != null ? req.userIsStudent() : true);
        u.setTitle(req.title());
        u.setPassword(passwordEncoder.encode(req.password()));
        return repo.save(u);
    }
}
