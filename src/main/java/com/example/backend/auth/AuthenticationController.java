package com.example.backend.auth;


import com.example.backend.data.AuthenticationRequest;
import com.example.backend.data.AuthenticationResponse;
import com.example.backend.data.PasswordResetQueryRequest;
import com.example.backend.data.PasswordResetRequest;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.attribute.UserPrincipalNotFoundException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService service;

    @PostMapping("/register")
    public String register(final HttpServletRequest request,
                           @RequestBody AuthenticationRequest authenticationRequest)
            throws MessagingException {
        return service.register(authenticationRequest, request.getHeader(HttpHeaders.USER_AGENT));
    }

    @PostMapping("/activate-account")
    public AuthenticationResponse activateAccount(@RequestParam("token") String token) {
        return service.getActivationCode(token);
    }

    @PostMapping("/auth")
    public AuthenticationResponse auth(final HttpServletRequest request,
                                       @RequestBody AuthenticationRequest authenticationRequest) {
        return service.authenticate(authenticationRequest, request.getHeader(HttpHeaders.USER_AGENT));
    }

    @RequestMapping(value = "/password-reset-query",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public String passwordResetQuery(final HttpServletRequest request,
                                     @RequestBody PasswordResetQueryRequest passwordResetQueryRequest)
            throws MessagingException, UserPrincipalNotFoundException {
        System.out.println("password-reset-query: controller");
        return service.forgotPassword(request, passwordResetQueryRequest.getEmail());
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody PasswordResetRequest passwordResetRequest,
                              @RequestParam("token") String token) {
        service.resetPassword(passwordResetRequest, token);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(HttpServletRequest request) throws IOException {
        return ResponseEntity.ok(service.refreshToken(request));
    }
}
