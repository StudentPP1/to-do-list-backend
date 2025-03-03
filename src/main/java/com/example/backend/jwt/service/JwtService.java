package com.example.backend.jwt.service;

import com.example.backend.enums.TokenType;
import com.example.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Function;

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
        return buildToken(user, accessTokenExpiration, TokenType.ACCESS_TOKEN);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenExpiration, TokenType.REFRESH_TOKEN);
    }

    public void setTokensToCookie(User user, HttpServletResponse response) {
        var accessToken = this.generateAccessToken(user);
        var refreshToken = this.generateRefreshToken(user);
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true);
        accessTokenCookie.setMaxAge(accessTokenExpiration);
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true);
        refreshTokenCookie.setSecure(true);
        accessTokenCookie.setMaxAge(refreshTokenExpiration);
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }

    public boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private String buildToken(
            User user,
            int expiration,
            TokenType type
    ) {
        var authorities = user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        var expirationDate = LocalDateTime.now()
                .plusMinutes(expiration)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return Jwts.builder()
                .claim("authorities", authorities)
                .claim("type", type)
                .setExpiration(Date.from(Instant.from(expirationDate)))
                .setIssuedAt(new Date())
                .setSubject(user.getEmail())
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
        System.out.println(token);
        return Jwts.parserBuilder()
                .setSigningKey(getSingInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
