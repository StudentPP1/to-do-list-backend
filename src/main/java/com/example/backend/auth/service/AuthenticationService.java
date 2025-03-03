package com.example.backend.auth.service;


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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.backend.user.User;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.Date;
import java.util.Optional;

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
        } catch (final UsernameNotFoundException e) {
            throw new IllegalAccessError("user has already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .build();

        userService.saveUser(user);
        return emailService.sendValidationEmail(user);
    }

    public AuthenticationResponse getActivationCode(String activationCode, HttpServletResponse response) {
        log.info("activation service is working");
        Token activationToken = tokenService.findByToken(activationCode);
        User user = userService.getUserById(activationToken.getUserId());

        if (activationToken.getExpiredAt().after(new Date())) {
            tokenService.deleteToken(activationToken);
            user.setEnabled(true);
            userService.saveUser(user);
            authenticateUser(user);

            var jwtToken = jwtService.generateAccessToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);
            jwtService.setRefreshTokenToCookie(refreshToken, response);
            return AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .build();
        }
        else {
            throw new RuntimeException("Activation code is expired");
        }
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("authenticate: call");
        User user = userService.getUserByEmail(request.getEmail());

        var jwtToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthenticationResponse refreshToken(HttpServletRequest request) throws Exception {
        log.info("refreshToken: call");
        Optional<Cookie> refreshTokenCookie = CookieUtils.getCookie(request, "refreshToken");
        String refreshToken = String.valueOf(refreshTokenCookie.orElseThrow(
                () -> new ServletException("refreshToken isn't present")
        ));
        User user = userService.getUser();
        return jwtService.validateAndSendTokens(user, refreshToken);
    }

    public String forgotPassword(String email) throws UserPrincipalNotFoundException, MessagingException {
        User user = userService.getUserByEmail(email);
        return emailService.sendForgotPasswordEmail(user);
    }

    public void resetPassword(PasswordResetRequest passwordResetRequest, String token) throws Exception {
        Token resetPasswordToken = tokenService.findByToken(token);
        if (resetPasswordToken.getExpiredAt().after(new Date())) {
            if (passwordResetRequest.getNewPassword().equals(passwordResetRequest.getConfirmPassword())) {
                User user = userService.getUserById(resetPasswordToken.getUserId());
                userService.resetPassword(user, passwordResetRequest.getNewPassword());
            }
        } else {
            throw new Exception("token is expired");
        }
        tokenService.deleteToken(resetPasswordToken);
    }

    public static void authenticateUser(User user) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}