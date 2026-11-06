package com.apartment.rental.account.service.impl;

import com.apartment.rental.account.dto.response.UserInfoForM04Response;
import com.apartment.rental.account.entity.Role;
import com.apartment.rental.account.mapper.UserMapperForM04;
import com.apartment.rental.account.repository.UserRepositoryForM04;
import com.apartment.rental.account.service.UserServiceForM04;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceForM04Impl implements UserServiceForM04 {

    private final UserRepositoryForM04 userRepository;
    private final UserMapperForM04 userMapper;

    @Override
    public List<UserInfoForM04Response> getAvailableTenants() {
        return userRepository.findAllByRoleOrderByFullNameAsc(Role.USER)
                .stream()
                .map(userMapper::toM04Response)
                .toList();
    }
}