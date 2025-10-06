package com.example.server.user;

public record UserDto(
        Long id,
        Long userName,
        String name,
        String surname,
        Boolean userIsStudent,
        String title
) {
    public static UserDto from(User u) {
        return new UserDto(
                u.getId(), u.getUserName(), u.getName(),
                u.getSurname(), u.isUserIsStudent(), u.getTitle()
        );
    }
}
