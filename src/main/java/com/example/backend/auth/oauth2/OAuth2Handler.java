package com.example.backend.auth.oauth2;

import com.example.backend.auth.service.AuthenticationService;
import com.example.backend.enums.Oauth2Connection;
import com.example.backend.jwt.service.JwtService;
import com.example.backend.enums.Role;
import com.example.backend.user.User;
import com.example.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2Handler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${spring.application.frontend.url}")
    private String FRONTEND_URL;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

        ArrayList<String> oauth2ConnectionList = new ArrayList<>();
        oauth2ConnectionList.add(Oauth2Connection.GOOGLE.name().toLowerCase());

        if (oauth2ConnectionList.contains(token.getAuthorizedClientRegistrationId())) {
            DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = principal.getAttributes();
            String password = "";
            String email = (String) attributes.getOrDefault("email", "");
            String name = (String) attributes.getOrDefault("name", "");

            User user = repository.findByEmail(email).orElseGet(() -> {
                log.info("OAuth2: user not found");
                User newUser = new User();
                newUser.setUsername(name);
                newUser.setEmail(email);
                newUser.setPassword(passwordEncoder.encode(password));
                newUser.setEnabled(true);
                newUser.setAccountLocked(false);
                newUser.setRole(Role.USER);
                userRepository.save(newUser);
                return newUser;
            });

            AuthenticationService.authenticateUser(user);
            jwtService.setTokensToCookie(user, response);

            getRedirectStrategy().sendRedirect(
                    request,
                    response,
                    "%s/oauth2/redirect".formatted(FRONTEND_URL)
            );
        }
    }
}