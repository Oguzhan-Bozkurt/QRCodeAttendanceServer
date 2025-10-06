package com.example.server.security;

import com.example.server.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Long userNameLong;
        try {
            userNameLong = Long.valueOf(username);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Kullanıcı adı sayısal olmalı");
        }

        var u = repo.findByUserName(userNameLong)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı"));

        // DİKKAT: Burada FQCN kullanıyoruz, import etmiyoruz.
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUserName().toString())
                .password(u.getPassword())
                .authorities("USER")
                .build();
    }
}
