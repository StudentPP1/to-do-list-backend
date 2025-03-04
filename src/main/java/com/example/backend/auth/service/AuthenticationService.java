package com.example.backend.auth.service;


import com.example.backend.enums.TokenType;
import com.example.backend.jwt.service.JwtService;
import com.example.backend.request.AuthenticationRequest;
import com.example.backend.response.AuthenticationResponse;
import com.example.backend.request.PasswordResetRequest;
import com.example.backend.email.EmailService;
import com.example.backend.token.Token;
import com.example.backend.token.TokenService;
import com.example.backend.user.UserService;
import com.example.backend.utils.CookieUtils;
import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.backend.user.User;

import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final UserService userService;
    private final TokenService tokenService;
    private final JwtService jwtService;

    public String register(AuthenticationRequest request) throws MessagingException {
        log.info("register service is working");

        try {
            userService.getUserByEmail(request.getEmail());
            throw new IllegalAccessError("user has already registered");
        } catch (final UsernameNotFoundException e) {
            User user = User.builder()
                    .email(request.getEmail())
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .accountLocked(false)
                    .enabled(false)
                    .build();
            userService.saveUser(user);
            return emailService.sendEmail(user, TokenType.ACTIVATE_ACCOUNT);
        }
    }

    public AuthenticationResponse getActivationCode(String activationCode, HttpServletResponse response) throws Exception {
        log.info("activation service is working");
        Token activationToken = validateAndGetToken(activationCode);
        User user = userService.getUserById(activationToken.getUserId());
        tokenService.deleteToken(activationToken);

        user.setEnabled(true);
        userService.saveUser(user);
        registerUser(user);

        var jwtToken = jwtService.generateAccessToken(user);
        jwtService.setRefreshTokenToCookie(user, response);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(
            @NonNull HttpServletResponse response,
            AuthenticationRequest request
    ) {
        log.info("authenticate: call");
        User user = userService.getUserByEmail(request.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var accessToken = jwtService.generateAccessToken(user);
        jwtService.setRefreshTokenToCookie(user, response);
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .build();
    }

    public AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("refreshToken: call");
        String refreshToken = CookieUtils.getCookie(request, "refreshToken").orElseThrow(
                () -> new ServletException("refreshToken isn't present")
        );
        User user = userService.getUser();
        return jwtService.validateAndSendTokens(user, refreshToken, response);
    }

    public String forgotPassword(String email) throws MessagingException {
        User user = userService.getUserByEmail(email);
        return emailService.sendEmail(user, TokenType.FORGOT_PASSWORD);
    }

    public void resetPassword(PasswordResetRequest passwordResetRequest, String token) throws Exception {
        Token resetPasswordToken = validateAndGetToken(token);
        if (passwordResetRequest.getNewPassword().equals(passwordResetRequest.getConfirmPassword())) {
            User user = userService.getUserById(resetPasswordToken.getUserId());
            userService.resetPassword(user, passwordResetRequest.getNewPassword());
        }
        tokenService.deleteToken(resetPasswordToken);
    }

    public static void registerUser(User user) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.getContext();
        SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        context.setAuthentication(authenticationToken);
        securityContextHolderStrategy.setContext(context);
    }

    private Token validateAndGetToken(String tokenContent) throws Exception {
        Token token = tokenService.findByToken(tokenContent);
        if (!token.getExpiredAt().after(new Date())) {
            throw new Exception("token is expired");
        }
        return token;
    }
}