package com.example.server.security;

import com.example.server.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        long userId;
        try {
            userId = Long.parseLong(username);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Kullanıcı adı (ID) sayısal olmalıdır: " + username, e);
        }

        com.example.server.user.User appUser = userRepo.findByUserName(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + userId));

        String role = appUser.isUserIsStudent() ? "ROLE_STUDENT" : "ROLE_ACADEMIC";

        return User.withUsername(appUser.getUserName().toString())
                .password(appUser.getPassword())
                .authorities(role)
                .build();
    }
}
