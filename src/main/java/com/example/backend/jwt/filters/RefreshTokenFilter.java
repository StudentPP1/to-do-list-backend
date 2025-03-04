package com.example.backend.jwt.filters;


import com.example.backend.jwt.service.JwtService;
import com.example.backend.users.user.UserService;
import com.example.backend.utils.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class RefreshTokenFilter extends TokenFilter {
    public RefreshTokenFilter(JwtService jwtService, UserService userService) {
        super(jwtService, userService);
    }

    @Override
    protected String getToken(@NonNull HttpServletRequest request) throws ServletException {
        log.info("Refresh token filter invoke");
        Optional<String> refreshTokenCookie = CookieUtils.getCookie(request, "refreshToken");
        return refreshTokenCookie.orElseThrow(() -> new ServletException("refreshToken isn't present"));
    }
}
