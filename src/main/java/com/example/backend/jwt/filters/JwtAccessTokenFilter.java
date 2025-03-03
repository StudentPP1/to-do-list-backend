package com.example.backend.jwt.filters;

import com.example.backend.jwt.service.JwtService;
import com.example.backend.user.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenFilter extends TokenFilter {
    public JwtAccessTokenFilter(JwtService jwtService, UserService userService) {
        super(jwtService, userService);
    }
    @Override
    protected String getToken(@NonNull HttpServletRequest request) throws ServletException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ServletException("");
        }

        return authHeader.substring(7);
    }
}


