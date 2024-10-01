package com.example.backend.request;

import lombok.Data;

@Data
public class PasswordResetRequest {
    private String newPassword;
    private String confirmPassword;
}
