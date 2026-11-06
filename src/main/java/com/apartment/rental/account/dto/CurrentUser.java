package com.apartment.rental.account.dto;

import com.apartment.rental.account.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser {
    private Long userId;
    private String email;
    private String fullName;
    private Role role;
}