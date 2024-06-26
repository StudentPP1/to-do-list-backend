package com.example.backend.auth;


import com.example.backend.config.JwtService;
import com.example.backend.data.AuthenticationRequest;
import com.example.backend.data.AuthenticationResponse;
import com.example.backend.data.PasswordResetRequest;
import com.example.backend.email.EmailService;
import com.example.backend.email.EmailTemplateName;
import com.example.backend.token.Token;
import com.example.backend.token.TokenRepository;
import com.example.backend.token.TokenType;
import com.example.backend.user.Role;
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
    private final TokenRepository tokenRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public String register(AuthenticationRequest request, String device) throws MessagingException {
        System.out.println("register service: working");

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalAccessError("user has already registered");
        }

        System.out.println(request);
        var user = new User();
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
        return sendValidationEmail(user, device);
    }

    public AuthenticationResponse getActivationCode(String activationCode) {
        Optional<Token> optionalToken = tokenRepository.findByToken(activationCode);
        if (optionalToken.isPresent()) {
            Token activationToken = optionalToken.get();
            User user = userRepository.findById(activationToken.getUserId()).orElseThrow(() ->
                    new UsernameNotFoundException("user not found"));
            String device = activationToken.getDevice();

            if (activationToken.getExpiredAt().after(new Date())) {
                user.setEnabled(true);
                userRepository.save(user);
                jwtService.revokeAllUserTokens(user.getId());
                var jwtToken = jwtService.generateToken(user, device);
                var refreshToken = jwtService.generateRefreshToken(user, device);
                tokenRepository.save(Token.builder()
                        .token(refreshToken)
                        .build());

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                tokenRepository.delete(activationToken);

                System.out.println(user);
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

    private String sendValidationEmail(User user, String device) throws MessagingException {
        String activationToken = generateActivationCode();
        var token = Token.builder()
                .token(activationToken)
                .userId(user.getId())
                .createdAt(new Date(System.currentTimeMillis()))
                .expiredAt(new Date(System.currentTimeMillis() + 900000)) // 15 min
                .device(device)
                .tokenType(TokenType.ACTIVATION_ACCOUNT)
                .build();
        tokenRepository.save(token);

        emailService.sendEmail(
                user.getEmail(),
                user.getName(),
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

    public AuthenticationResponse authenticate(AuthenticationRequest request, String device) {
        System.out.println("authenticate: call");
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() ->
                new UsernameNotFoundException("user not found"));

        jwtService.revokeAllUserTokens(user.getId());

        var jwtToken = jwtService.generateToken(user, device);
        var refreshToken = jwtService.generateRefreshToken(user, device);
        tokenRepository.save(Token.builder()
                .token(refreshToken)
                .build());

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
        final String refreshToken;
        final String email;
        final String device;
        final String userCurrentDevice = request.getHeader(HttpHeaders.USER_AGENT);

        System.out.println(authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtException("token not found");
        }

        refreshToken = authHeader.substring(7);

        if (tokenRepository.findByToken(refreshToken).isEmpty()) {
            throw new JwtException("token not found");
        }

        email = jwtService.extractEmail(refreshToken);
        device = jwtService.extractDevice(refreshToken);

        if (email != null && device.equals(userCurrentDevice)) {
            User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user not found"));

            if (jwtService.isTokenValid(refreshToken, user)) {
                jwtService.revokeAllUserTokens(user.getId());
                String newToken = jwtService.generateToken(user, device);
                String newRefreshToken = jwtService.generateRefreshToken(user, device);

                tokenRepository.save(Token.builder()
                        .token(newRefreshToken)
                        .build());

                return AuthenticationResponse.builder()
                        .accessToken(newToken)
                        .refreshToken(newRefreshToken)
                        .build();
            }
        }

        return null;
    }

    public String forgotPassword(HttpServletRequest request, String email) throws MessagingException, UserPrincipalNotFoundException {
        System.out.println("forgot password service: working");
        String device = request.getHeader(HttpHeaders.USER_AGENT);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserPrincipalNotFoundException("user not found"));
        String generatedCode = generateActivationCode();

        Token token = Token.builder()
                .token(generatedCode)
                .userId(user.getId())
                .createdAt(new Date(System.currentTimeMillis()))
                .expiredAt(new Date(System.currentTimeMillis() + 900000)) // 15 min
                .device(device)
                .tokenType(TokenType.FORGOT_PASSWORD)
                .build();

        tokenRepository.save(token);

        emailService.sendEmail(
                user.getEmail(),
                user.getName(),
                EmailTemplateName.FORGOT_PASSWORD,
                generatedCode,
                "Forgot password"
        );
        return generatedCode;
    }

    public void resetPassword(PasswordResetRequest passwordResetRequest, String token) {
        Token resetPasswordToken = tokenRepository.findByToken(token).orElseThrow(() -> new JwtException("token not found"));
        if (resetPasswordToken.getExpiredAt().after(new Date())) {
            if (passwordResetRequest.getNewPassword().equals(passwordResetRequest.getConfirmPassword())) {
                User user = userRepository.findById(resetPasswordToken.getUserId()).orElseThrow(
                        () -> new UsernameNotFoundException("user not found"));
                userService.resetPassword(user, passwordResetRequest.getNewPassword());
                tokenRepository.delete(resetPasswordToken);
                tokenRepository.deleteByToken(token);
            }
        }

    }
}

