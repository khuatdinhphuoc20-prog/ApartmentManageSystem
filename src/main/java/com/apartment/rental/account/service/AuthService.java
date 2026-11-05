package com.apartment.rental.account.service;

import com.apartment.rental.account.dto.RegisterRequest;

public interface AuthService {
    boolean register(RegisterRequest request);
}