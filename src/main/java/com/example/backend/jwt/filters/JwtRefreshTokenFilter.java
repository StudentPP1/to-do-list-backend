package com.example.backend.jwt.filters;


import com.example.backend.jwt.service.JwtService;
import com.example.backend.user.UserService;
import com.example.backend.utils.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JwtRefreshTokenFilter extends TokenFilter {
    public JwtRefreshTokenFilter(JwtService jwtService, UserService userService) {
        super(jwtService, userService);
    }

    @Override
    protected String getToken(@NonNull HttpServletRequest request) throws ServletException {
        Optional<Cookie> refreshTokenCookie = CookieUtils.getCookie(request, "refreshToken");
        return String.valueOf(refreshTokenCookie.orElseThrow(
                () -> new ServletException("refreshToken isn't present")
                ));
    }
}
