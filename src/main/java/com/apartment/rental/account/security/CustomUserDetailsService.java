package com.apartment.rental.account.security;

import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.trim().isEmpty()) {
            throw new UsernameNotFoundException("Email không được để trống!");
        }

        String trimmedEmail = email.trim();

        User user = userRepository.findByEmail(trimmedEmail)
                .filter(u -> u.getEmail().equals(trimmedEmail))
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại: " + trimmedEmail));

        return new CustomUserDetails(user);
    }
}