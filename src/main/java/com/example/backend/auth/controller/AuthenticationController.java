package com.example.backend.auth.controller;


import com.example.backend.auth.service.AuthenticationService;
import com.example.backend.request.AuthenticationRequest;
import com.example.backend.response.AuthenticationResponse;
import com.example.backend.request.PasswordResetQueryRequest;
import com.example.backend.request.PasswordResetRequest;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public AuthenticationResponse activateAccount(
            @NonNull HttpServletResponse response,
            @RequestParam("token") String token
    ) throws Exception {
        return service.getActivationCode(token, response);
    }

    @PostMapping("/auth")
    public AuthenticationResponse auth(
            @NonNull HttpServletResponse response,
            @RequestBody AuthenticationRequest authenticationRequest) {
        return service.authenticate(response, authenticationRequest);
    }

    @RequestMapping(value = "/password-reset-query",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public String passwordResetQuery(@RequestBody PasswordResetQueryRequest passwordResetQueryRequest)
            throws MessagingException {
        return service.forgotPassword(passwordResetQueryRequest.getEmail());
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody PasswordResetRequest passwordResetRequest,
                              @RequestParam("token") String token) throws Exception {
        service.resetPassword(passwordResetRequest, token);
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            HttpServletResponse response,
            HttpServletRequest request) throws Exception {
        return ResponseEntity.ok(service.refreshToken(request, response));
    }
}
