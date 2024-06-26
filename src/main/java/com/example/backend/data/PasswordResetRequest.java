package com.example.backend.data;

import lombok.Data;

@Data
public class PasswordResetRequest {
    private String newPassword;
    private String confirmPassword;
}
