package com.apartment.rental.account.service;

import com.apartment.rental.account.dto.UpdateProfileRequest;
import com.apartment.rental.account.dto.UserSummary;
import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.entity.User;
import com.apartment.rental.account.exception.BusinessException;
import com.apartment.rental.account.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UpdateProfileRequest getProfileByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy thông tin người dùng!"));

        return new UpdateProfileRequest(
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getIdentityDocumentNo()
        );
    }

    @Override
    @Transactional
    public boolean updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "Không tìm thấy thông tin người dùng!"));

        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone().trim());
        user.setIdentityDocumentNo(request.getIdentityDocumentNo().trim());

        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> findUserSummaryById(Long userId) {
        return userRepository.findById(userId).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> getAllUsersForContract() {
        return userRepository.findByRoleOrderByFullNameAsc(Role.USER)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getIdentityDocumentNo(),
                user.getRole()
        );
    }
}