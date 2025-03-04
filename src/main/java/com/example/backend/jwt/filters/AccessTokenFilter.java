package com.example.backend.jwt.filters;

import com.example.backend.jwt.service.JwtService;
import com.example.backend.users.user.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccessTokenFilter extends TokenFilter {
    public AccessTokenFilter(JwtService jwtService, UserService userService) {
        super(jwtService, userService);
    }
    @Override
    protected String getToken(@NonNull HttpServletRequest request) throws ServletException {
        log.info("Access token filter invoke");
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ServletException("token not found");
        }

        return authHeader.substring(7);
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.endsWith("/auth/refresh-token");
    }
}


