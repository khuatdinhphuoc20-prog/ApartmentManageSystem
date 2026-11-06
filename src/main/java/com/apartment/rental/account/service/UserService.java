package com.apartment.rental.account.service;

import com.apartment.rental.account.dto.UpdateProfileRequest;
import com.apartment.rental.account.dto.UserSummary;
import java.util.List;
import java.util.Optional;

public interface UserService {

    UpdateProfileRequest getProfileByUserId(Long userId);

    boolean updateProfile(Long userId, UpdateProfileRequest request);

    Optional<UserSummary> findUserSummaryById(Long userId);

    List<UserSummary> getAllUsersForContract();
}