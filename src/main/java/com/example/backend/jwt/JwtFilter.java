package com.example.backend.jwt;


import com.example.backend.user.User;
import com.example.backend.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserService userService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String jwt;
        final String email;
        final String device;
        final String userCurrentDevice;

        System.out.println("authHeader: " + authHeader);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        email = jwtService.extractEmail(jwt);
        device = jwtService.extractDevice(jwt);
        userCurrentDevice = request.getHeader(HttpHeaders.USER_AGENT);

        System.out.println("email: " + email);
        System.out.println("device: " + device);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null && device.equals(userCurrentDevice)) {
            System.out.println("jwt filter: checked");
            User user = userService.loadUserByUsername(email);
            if (jwtService.isTokenValid(jwt, user)) {
                System.out.println("jwt filter: token is valid");
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );
                token.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(token);
            }
        }
        else {
            System.out.println("jwt filter: do other filter");
        }
        filterChain.doFilter(request, response);
    }
}


