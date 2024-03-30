package com.example.backend.config;


import com.example.backend.token.TokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor // конструктор з private final filed
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, // наш запит
            @NonNull HttpServletResponse response, // наша відповідь
            @NonNull FilterChain filterChain // список фильтров
    ) throws ServletException, IOException {
        // our token in header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        // check token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        jwt = authHeader.substring(7); // get jwt token
        // get user email from token (using JwtService)
        userEmail = jwtService.extractUsername(jwt);

        // якщо є логін та користувач не зареестрований
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // get user from db
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            // fix bug with access to demo-controller with old token after generate a new one
            var isTokenValid = tokenRepository.findByToken(jwt) // find token object by token
                    .map(t -> !t.isExpired() && !t.isRevoked()) // it should be not expired and not revoked => return true
                    .orElse(false);
            // valid token
            if (jwtService.isTokenValid(jwt, userDetails) && isTokenValid) {
                // update security context
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                // set details of our request
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                // update auth token
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
            // pass next filter
            filterChain.doFilter(request, response);
        }
    }
}

