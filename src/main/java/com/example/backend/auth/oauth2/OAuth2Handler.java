package com.example.backend.auth.oauth2;

import com.example.backend.auth.service.AuthenticationService;
import com.example.backend.jwt.service.JwtService;
import com.example.backend.users.connectedAccount.UserConnectedAccount;
import com.example.backend.users.connectedAccount.UserConnectedAccountRepository;
import com.example.backend.users.user.User;
import com.example.backend.users.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2Handler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final UserService userService;
    private final UserConnectedAccountRepository userConnectedAccountRepository;
    private final JwtService jwtService;

    @Value("${spring.application.frontend.url}")
    private String FRONTEND_URL;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String providerId = token.getAuthorizedClientRegistrationId(); // Google / GitHub
        String name = authentication.getName(); // username on provider
        String email = token.getPrincipal().getAttribute("email");
        // check if you have user based on this account
        Optional<UserConnectedAccount> connectedAccount = userConnectedAccountRepository.findByProviderIdAndName(
                providerId, name
        );
        if (connectedAccount.isPresent()) {
            User user = userService.getUserById(connectedAccount.get().getUserId());
            registerUserAndRedirect(user, request, response);
            return;
        }
        // find user by email & add connect user or create a new user
        try {
            User user = userService.getUserByEmail(email);
            registerUserAndRedirect(user, request, response);
        } catch (Exception e) {
            log.info("OAuth2: user not found");
            User user = new User();
            user.setUsername(name);
            user.setEmail(email);
            user.setPassword("");
            user.setEnabled(true);
            user.setAccountLocked(false);
            user = userService.saveUser(user);
            UserConnectedAccount newConnectedAccount = new UserConnectedAccount(
                    providerId, name, user.getId()
            );
            userConnectedAccountRepository.save(newConnectedAccount);
            registerUserAndRedirect(user, request, response);
        }
    }
    private void registerUserAndRedirect(
            User user,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        AuthenticationService.registerUser(user);
        jwtService.setRefreshTokenToCookie(user, response);
        log.info("OAuth2: sent tokens");
        getRedirectStrategy().sendRedirect(
                request,
                response,
                "%s/oauth2/redirect".formatted(FRONTEND_URL)
        );
    }
}