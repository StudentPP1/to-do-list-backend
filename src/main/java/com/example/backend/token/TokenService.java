package com.example.backend.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
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
}
