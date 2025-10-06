package com.example.server.user;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(Long userName) {
        super("Kullanıcı adı zaten var: " + userName);
    }
}
