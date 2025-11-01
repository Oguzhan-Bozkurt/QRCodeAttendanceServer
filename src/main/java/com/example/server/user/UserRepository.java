package com.example.server.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserName(Long userName);
    boolean existsByUserName(Long userName);
    List<User> findAllByUserIsStudentTrue();
}
