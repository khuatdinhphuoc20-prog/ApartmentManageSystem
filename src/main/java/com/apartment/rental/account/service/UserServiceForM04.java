package com.apartment.rental.account.service;

import com.apartment.rental.account.dto.response.UserInfoForM04Response;

import java.util.List;

public interface UserServiceForM04 {

    List<UserInfoForM04Response> getAvailableTenants();
}