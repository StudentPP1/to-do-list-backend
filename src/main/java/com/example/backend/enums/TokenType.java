package com.example.backend.enums;

import lombok.Getter;

@Getter
public enum TokenType {
    ACTIVATE_ACCOUNT("activate_account"),
    FORGOT_PASSWORD("forgot_password");
    private final String name;
    TokenType(String name) {
        this.name = name;
    }
}

