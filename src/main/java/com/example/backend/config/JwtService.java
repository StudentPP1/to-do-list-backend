package com.example.backend.config;

import com.example.backend.token.TokenRepository;
import com.example.backend.token.TokenType;
import com.example.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${spring.application.security.jwt.secret-key}")
    public String secretKey;
    @Value("${spring.application.security.jwt.expiration}")
    public long expiration;
    @Value("${spring.application.security.jwt.refresh-token.expiration}")
    public long refreshTokenExpiration;

    private final TokenRepository repository;

    public String generateToken(User user, String device) {
        return generateToken(new HashMap<>(), user, device);
    }

    public String generateToken(Map<String, String> claims, User user, String device) {
        return buildToken(claims, user, expiration, device, TokenType.ACCESS_TOKEN);
    }

    public String generateRefreshToken(User user, String device) {
        return buildToken(new HashMap<>(), user, refreshTokenExpiration, device, TokenType.REFRESH_TOKEN);
    }

    public String buildToken(
            Map<String, String> claims,
            User user,
            long expiration,
            String device,
            TokenType type
    ) {
        var authorities = user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .claim("authorities", authorities)
                .claim("device", device)
                .claim("type", type)
                .claim("userId", user.getId())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .setIssuedAt(new Date())
                .setSubject(user.getEmail())
                .signWith(getSingInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Key getSingInKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isTokenValid(String token, User user) {
        final String email = extractEmail(token);
        return (email.equals(user.getEmail())) && !isTokenExpired(token);
    }

    public String extractUser(String token) {
        Claims claims = getAllClaims(token);
        return (String) claims.get("userId");
    }

    public String extractType(String token) {
        Claims claims = getAllClaims(token);
        return (String) claims.get("type");
    }

    public String extractDevice(String token) {
        Claims claims = getAllClaims(token);
        return (String) claims.get("device");
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaims(String token) {
        System.out.println(Jwts.parserBuilder()
                .setSigningKey(getSingInKey())
                .build()
                .parse(token)
        );
        return Jwts.parserBuilder()
                .setSigningKey(getSingInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public void revokeAllUserTokens(String userId) {
        var userToken = repository.findTokensByUserId(userId);

        if (userToken.isEmpty()) {
            return;
        }

        userToken.forEach(t -> {
            repository.deleteById(t.getId());
        });
    }
}
