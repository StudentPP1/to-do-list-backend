package com.example.backend.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
    ACTIVATE_ACCOUNT("activate_account"),
    FORGOT_PASSWORD("forgot_password");

    private String name;

    EmailTemplateName(String name) {
        this.name = name;
    }
}
