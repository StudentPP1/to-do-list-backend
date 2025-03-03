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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public abstract class TokenFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserService userService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = this.getToken(request);
        this.validateToken(request, response, filterChain, token, jwtService, userService);
    }
    protected abstract String getToken(@NonNull HttpServletRequest request) throws ServletException;

    protected void validateToken(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain,
            String jwt,
            JwtService jwtService,
            UserService userService
    ) throws IOException, ServletException {
        String email = jwtService.extractEmail(jwt);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.info("jwt filter: checked");
            User user = userService.loadUserByUsername(email);
            if (jwtService.isTokenValid(jwt)) {
                log.info("jwt filter: token is valid");
                AuthenticationService.authenticateUser(user);
            }
        }
        else {
            log.info("jwt filter: do other filter");
        }
        filterChain.doFilter(request, response);
    }
}
