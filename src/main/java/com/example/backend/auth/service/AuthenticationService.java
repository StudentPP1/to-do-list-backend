package com.example.backend.auth.service;


import com.example.backend.jwt.service.JwtService;
import com.example.backend.request.AuthenticationRequest;
import com.example.backend.response.AuthenticationResponse;
import com.example.backend.request.PasswordResetRequest;
import com.example.backend.email.EmailService;
import com.example.backend.email.EmailTemplateName;
import com.example.backend.token.Token;
import com.example.backend.enums.TokenType;
import com.example.backend.enums.Role;
import com.example.backend.token.TokenRepository;
import com.example.backend.user.UserRepository;
import com.example.backend.user.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.backend.user.User;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserService userService;
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public String register(AuthenticationRequest request) throws MessagingException {
        System.out.println("register service: working");

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalAccessError("user has already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAccountLocked(false);
        user.setEnabled(false);
        user.setRole(Role.USER);
        user.setDoneTasksId(new ArrayList<>());
        user.setTasksId(new ArrayList<>());
        user.setTagsId(new ArrayList<>());

        System.out.println(user);

        userRepository.save(user);
        return sendValidationEmail(user);
    }

    public AuthenticationResponse getActivationCode(String activationCode) {
        System.out.println("activation service is working");
        Optional<Token> optionalToken = tokenRepository.findByToken(activationCode);
        if (optionalToken.isPresent()) {
            Token activationToken = optionalToken.get();
            User user = userRepository.findById(activationToken.getUserId()).orElseThrow(() ->
                    new UsernameNotFoundException("user not found"));

            if (activationToken.getExpiredAt().after(new Date())) {
                user.setEnabled(true);
                userRepository.save(user);
                var jwtToken = jwtService.generateAccessToken(user);
                var refreshToken = jwtService.generateRefreshToken(user);

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                tokenRepository.delete(activationToken);

                return AuthenticationResponse.builder()
                        .accessToken(jwtToken)
                        .refreshToken(refreshToken)
                        .build();
            }
            else {
                throw new RuntimeException("Activation code is expired");
            }
        }
        else {
            throw new RuntimeException("Invalid activation code");
        }
    }

    private String sendValidationEmail(User user) throws MessagingException {
        String activationToken = generateActivationCode();
        var token = Token.builder()
                .token(activationToken)
                .userId(user.getId())
                .createdAt(new Date(System.currentTimeMillis()))
                .expiredAt(new Date(System.currentTimeMillis() + 900000)) // 15 min
                .tokenType(TokenType.ACTIVATION_ACCOUNT)
                .build();

        tokenRepository.save(token);

        emailService.sendEmail(
                user.getEmail(),
                user.getUsername(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationToken,
                "Activation account"
        );
        return activationToken;
    }

    private String generateActivationCode() {
        String numbers = "0123456789";
        StringBuilder newCode = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < 6; i++) {
            newCode.append(numbers.charAt(random.nextInt(numbers.length())));
        }

        return newCode.toString();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        System.out.println("authenticate: call");
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() ->
                new UsernameNotFoundException("user not found"));

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

    public AuthenticationResponse refreshToken(HttpServletRequest request) {
        System.out.println("refreshToken: call");
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtException("token not found");
        }

        String refreshToken = authHeader.substring(7);
        String email = jwtService.extractEmail(refreshToken);

        if (email != null) {
            User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user not found"));

            if (jwtService.isTokenValid(refreshToken)) {
                String newToken = jwtService.generateAccessToken(user);
                String newRefreshToken = jwtService.generateRefreshToken(user);

                System.out.println("refreshToken: send new tokens");
                return AuthenticationResponse.builder()
                        .accessToken(newToken)
                        .refreshToken(newRefreshToken)
                        .build();
            }
        }

        return null;
    }

    public String forgotPassword(String email) throws MessagingException, UserPrincipalNotFoundException {
        System.out.println("forgot password service: working");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserPrincipalNotFoundException("user not found"));
        String generatedCode = generateActivationCode();

        Token token = Token.builder()
                .token(generatedCode)
                .userId(user.getId())
                .createdAt(new Date(System.currentTimeMillis()))
                .expiredAt(new Date(System.currentTimeMillis() + 900000)) // 15 min
                .tokenType(TokenType.FORGOT_PASSWORD)
                .build();

        tokenRepository.save(token);

        emailService.sendEmail(
                user.getEmail(),
                user.getUsername(),
                EmailTemplateName.FORGOT_PASSWORD,
                generatedCode,
                "Forgot password"
        );
        return generatedCode;
    }

    public void resetPassword(PasswordResetRequest passwordResetRequest, String token) throws Exception {
        Token resetPasswordToken = tokenRepository.findByToken(token).orElseThrow(() -> new JwtException("token not found"));
        if (resetPasswordToken.getExpiredAt().after(new Date())) {
            if (passwordResetRequest.getNewPassword().equals(passwordResetRequest.getConfirmPassword())) {
                User user = userRepository.findById(resetPasswordToken.getUserId()).orElseThrow(
                        () -> new UsernameNotFoundException("user not found"));
                userService.resetPassword(user, passwordResetRequest.getNewPassword());
            }
        } else {
            throw new Exception("token is expired");
        }
        tokenRepository.delete(resetPasswordToken);
        tokenRepository.deleteByToken(token);
    }
}

