package com.apartment.rental.account.dto;

import com.apartment.rental.account.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String identityDocumentNo;
    private Role role;
}