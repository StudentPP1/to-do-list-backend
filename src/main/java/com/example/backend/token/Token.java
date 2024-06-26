package com.example.backend.token;

import com.example.backend.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tokens")
public class Token {
    @Id
    private String id;
    private String device;
    private String token;
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;
    private Date expiredAt;
    private Date createdAt;
    private String userId;
}
