package com.example.backend.auth.controller;


import com.example.backend.auth.service.AuthenticationService;
import com.example.backend.request.AuthenticationRequest;
import com.example.backend.response.AuthenticationResponse;
import com.example.backend.request.PasswordResetQueryRequest;
import com.example.backend.request.PasswordResetRequest;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipalNotFoundException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService service;

    @PostMapping("/register")
    public String register(@RequestBody AuthenticationRequest authenticationRequest)
            throws MessagingException {
        return service.register(authenticationRequest);
    }

    @PostMapping("/activate-account")
    public AuthenticationResponse activateAccount(@RequestParam("token") String token) {
        return service.getActivationCode(token);
    }

    @PostMapping("/auth")
    public AuthenticationResponse auth(@RequestBody AuthenticationRequest authenticationRequest) {
        return service.authenticate(authenticationRequest);
    }

    @RequestMapping(value = "/password-reset-query",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public String passwordResetQuery(@RequestBody PasswordResetQueryRequest passwordResetQueryRequest)
            throws MessagingException, UserPrincipalNotFoundException {
        System.out.println("password-reset-query: controller");
        return service.forgotPassword(passwordResetQueryRequest.getEmail());
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody PasswordResetRequest passwordResetRequest,
                              @RequestParam("token") String token) throws Exception {
        service.resetPassword(passwordResetRequest, token);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(HttpServletRequest request) {
        return ResponseEntity.ok(service.refreshToken(request));
    }
}
