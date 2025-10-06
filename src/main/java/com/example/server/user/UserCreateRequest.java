package com.example.server.user;

import jakarta.validation.constraints.*;

public record UserCreateRequest(
        @NotNull Long userName,
        @NotBlank @Size(min = 6, max = 60) String password,
        @NotBlank String name,
        @NotBlank String surname,
        Boolean userIsStudent,
        String title
) {}
