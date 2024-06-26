package com.example.backend.token;

import com.example.backend.user.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends MongoRepository<Token, String> {
    Optional<Token> findByToken(String token);
    List<Token> findTokensByUserId(String userId);
    void deleteByToken(String token);
}

