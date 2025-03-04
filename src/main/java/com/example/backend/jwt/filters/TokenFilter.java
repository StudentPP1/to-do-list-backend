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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
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
        try {
            String token = this.getToken(request);
            this.validateToken(request, response, filterChain, token);
        } catch (final Exception e) {
            filterChain.doFilter(request, response);
        }
    }

    protected abstract String getToken(
            @NonNull HttpServletRequest request
    ) throws ServletException;

    protected void validateToken(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain,
            String token
    ) throws IOException, ServletException {
        String email = jwtService.extractEmail(token);
        log.info("TokenFilter: get email from token");
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.info("TokenFilter: find user by email");
            User user = userService.getUserByEmail(email);
            log.info("TokenFilter: validation token");
            if (jwtService.isTokenValid(token)) {
                log.info("TokenFilter: register user in context");
                AuthenticationService.registerUser(user);
            }
        }
        filterChain.doFilter(request, response);
    }
}
