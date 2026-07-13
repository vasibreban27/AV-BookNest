package com.avbooknest.auth.dto;

import com.avbooknest.auth.model.User;

public record UserResponse(Long id, String firstName, String lastName, String email, String role, boolean emailVerified) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().getName(),
                user.isEmailVerified()
        );
    }
}
