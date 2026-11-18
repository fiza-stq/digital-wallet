
package com.example.digital_wallet.mapper;

import com.example.digital_wallet.dto.response.UserResponse;
import com.example.digital_wallet.entity.User;

import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }
}