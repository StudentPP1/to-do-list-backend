package com.example.backend.oauth2;

import com.example.backend.jwt.JwtService;
import com.example.backend.token.Token;
import com.example.backend.token.TokenRepository;
import com.example.backend.user.Role;
import com.example.backend.user.User;
import com.example.backend.user.UserRepository;
import com.example.backend.utils.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class OAuth2Handler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;


    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

        ArrayList<String> oauth2ConnectionList = new ArrayList<>();
        oauth2ConnectionList.add("github");
        oauth2ConnectionList.add("google");

        if (oauth2ConnectionList.contains(token.getAuthorizedClientRegistrationId())) {
            DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = principal.getAttributes();
            System.out.println(attributes);
            String password = "";
            String email = (String) attributes.getOrDefault("email", "");
            String name = (String) attributes.getOrDefault("name", "");
            String device = request.getHeader(HttpHeaders.USER_AGENT);

            System.out.println(email);
            System.out.println(name);
            System.out.println(device);

            System.out.println("oauth2Connection: " + token.getAuthorizedClientRegistrationId());

            AtomicReference<User> user = new AtomicReference<>();
            System.out.println(repository.findAll());
            repository.findByEmail(email).ifPresentOrElse(
                    findUser -> {
                        System.out.println("user found");
                        user.set(findUser);
                    },
                    () -> {
                        System.out.println("user NOT found");
                        User newUser = new User();
                        newUser.setUsername(name);
                        newUser.setEmail(email);
                        newUser.setPassword(passwordEncoder.encode(password));
                        newUser.setEnabled(true);
                        newUser.setAccountLocked(false);
                        newUser.setRole(Role.USER);
                        user.set(newUser);
                        userRepository.save(newUser);
                    });

            HashMap<String, String> tokens = getTokens(user.get(), device);

            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers", "x-requested-with, authorization, content-type");

            String targetUrl = determineTargetUrl(request, response, tokens.get("refreshToken"));
            System.out.println(user);
            System.out.println("targetUrl: " + targetUrl);

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

//            getRedirectStrategy().sendRedirect(request, response, "http://localhost:3000/auth/callback?refreshToken=" + tokens.get("refreshToken"));
        }
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, String token) throws BadRequestException, UnexpectedException {
        Optional<String> redirectUri = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        return UriComponentsBuilder.fromUriString(redirectUri.orElseThrow(() ->
                        new UnexpectedException("redirectUri is missed")))
                .queryParam("token", token)
                .build().toUriString();
    }


    private HashMap<String, String> getTokens(User user, String device) {
        HashMap<String, String> tokens = new HashMap<>();
        jwtService.revokeAllUserTokens(user.getId());
        var jwtToken = jwtService.generateToken(user, device);
        var refreshToken = jwtService.generateRefreshToken(user, device);
        tokenRepository.save(Token.builder()
                .token(refreshToken)
                .userId(user.getId())
                .build());

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        tokens.put("jwtToken", jwtToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }
}