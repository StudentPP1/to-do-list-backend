package com.example.backend.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Optional;

public class CookieUtils {
    public static Optional<String> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie).map(Cookie::getValue);
                }
            }
        }
        return Optional.empty();
    }

    public static void deleteCookies(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Arrays.stream(request.getCookies()).forEach(cookie -> {
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
        });
    }
}
