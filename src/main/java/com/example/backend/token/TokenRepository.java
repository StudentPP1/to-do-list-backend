package com.example.backend.token;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends MongoRepository<Token, String> {
    // all valid tokens for user

    List<Token> findTokensByUserId(String userId);

    Optional<Token> findByToken(String token);
}

