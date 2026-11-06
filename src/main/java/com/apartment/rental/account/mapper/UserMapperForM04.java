package com.apartment.rental.account.mapper;

import com.apartment.rental.account.dto.response.UserInfoForM04Response;
import com.apartment.rental.account.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapperForM04 {

    public UserInfoForM04Response toM04Response(User user) {
        return new UserInfoForM04Response(
                user.getId(),
                user.getFullName(),
                user.getIdentityDocumentNo(),
                user.getPhone()
        );
    }
}