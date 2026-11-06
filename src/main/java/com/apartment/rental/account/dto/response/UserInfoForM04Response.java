package com.apartment.rental.account.dto.response;

public record UserInfoForM04Response(
        Long id,
        String fullName,
        String identityDocumentNo,
        String phone
) {
}