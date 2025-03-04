package com.example.backend.jwt.service;

import com.example.backend.response.AuthenticationResponse;
import com.example.backend.users.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${spring.application.security.jwt.secret-key}")
    public String secretKey;
    @Value("${spring.application.security.jwt.expiration}")
    public int accessTokenExpiration;
    @Value("${spring.application.security.jwt.refresh-token.expiration}")
    public int refreshTokenExpiration;

    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenExpiration);
    }

    private String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenExpiration);
    }

    public void setRefreshTokenToCookie(User user, HttpServletResponse response) {
        var refreshToken = this.generateRefreshToken(user);
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(refreshTokenExpiration);
        refreshTokenCookie.setAttribute("SameSite", "Strict");
        log.info("refreshToken: send new tokens");
        response.addCookie(refreshTokenCookie);
    }

    public AuthenticationResponse validateAndSendTokens(User user, String token, HttpServletResponse response) throws AccessDeniedException {
        this.isTokenValid(token);
        String newAccessToken = this.generateAccessToken(user);
        this.setRefreshTokenToCookie(user, response);
        log.info("refreshToken: send new tokens");
        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    public boolean isTokenValid(String token) throws AccessDeniedException {
        if (isTokenExpired(token)) {
            throw new AccessDeniedException("token expired");
        }
        return true;
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private String buildToken(
            User user,
            long expiration
    ) {
        return Jwts.builder()
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .setIssuedAt(new Date())
                .setSubject(user.getId())
                .signWith(getSingInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSingInKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSingInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
