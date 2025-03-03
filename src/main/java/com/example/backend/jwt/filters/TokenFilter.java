package com.example.backend.jwt.filters;

import com.example.backend.auth.service.AuthenticationService;
import com.example.backend.jwt.service.JwtService;
import com.example.backend.user.User;
import com.example.backend.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public abstract class TokenFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserService userService;

    protected TokenFilter(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = this.getToken(request);
        this.validateToken(request, response, filterChain, token);
    }

    protected abstract String getToken(@NonNull HttpServletRequest request) throws ServletException;

    protected void validateToken(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain,
            String token
    ) throws IOException, ServletException {
        String email = jwtService.extractEmail(token);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = userService.getUserByEmail(email);
            if (jwtService.isTokenValid(token)) {
                AuthenticationService.authenticateUser(user);
            }
        }
        filterChain.doFilter(request, response);
    }
}
