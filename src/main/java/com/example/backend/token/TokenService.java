package com.example.backend.token;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final TokenRepository tokenRepository;

    public Token findByToken(String activationCode) {
        return tokenRepository.findByToken(activationCode)
                .orElseThrow(() -> new RuntimeException("Invalid activation code"));
    }

    public void deleteToken(Token token) {
        tokenRepository.delete(token);
    }

    public void deleteTokens(List<Token> tokens) {
        tokenRepository.deleteAll(tokens);
    }
}
