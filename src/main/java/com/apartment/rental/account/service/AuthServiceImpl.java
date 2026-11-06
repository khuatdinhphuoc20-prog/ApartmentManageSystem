package com.apartment.rental.account.service;

import com.apartment.rental.account.dto.RegisterRequest;
import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.exception.BusinessException;
import com.apartment.rental.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public boolean register(RegisterRequest request) {
        String email = request.getEmail().trim();
        String fullName = request.getFullName().trim();
        String phone = request.getPhone().trim();
        String identityDocumentNo = request.getIdentityDocumentNo().trim();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "Mật khẩu xác nhận không khớp!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(400, "Email đã tồn tại trên hệ thống!");
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setFullName(fullName);
        newUser.setPhone(phone);
        newUser.setIdentityDocumentNo(identityDocumentNo);
        newUser.setRole(Role.USER);

        userRepository.save(newUser);

        return true;
    }
}