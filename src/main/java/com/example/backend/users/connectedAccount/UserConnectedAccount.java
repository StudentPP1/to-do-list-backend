package com.example.backend.users.connectedAccount;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@Document
public class UserConnectedAccount {
    @Id
    @GeneratedValue
    private String id;
    private String providerId;
    private String name;
    private String userId;

    public UserConnectedAccount(String providerId, String name, String userId) {
        this.providerId = providerId;
        this.name = name;
        this.userId = userId;
    }
}
