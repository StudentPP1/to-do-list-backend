package com.example.backend.config;


import com.example.backend.jwt.filters.AccessTokenFilter;
import com.example.backend.jwt.filters.RefreshTokenFilter;
import com.example.backend.auth.oauth2.OAuth2Handler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final OAuth2Handler oAuth2Handler;
    private final AuthenticationProvider authenticationProvider;
    private final AuthenticationConfiguration authConfiguration;
    private final AccessTokenFilter accessTokenFilter;
    private final RefreshTokenFilter refreshTokenFilter;

    @Value("${spring.application.frontend.url}")
    private String FRONTEND_URL;

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authConfiguration.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(FRONTEND_URL));
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", configuration);
        return urlBasedCorsConfigurationSource;
    }

    @Bean
    SecurityFilterChain endpointsFilter(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(req ->
                        req
                                .requestMatchers( "/favicon.ico").permitAll()
                                .requestMatchers("/logout").permitAll()
                                .requestMatchers("/oauth2/**").permitAll()
                                .requestMatchers("/auth/**").permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2Login(auth ->
                        {
                            auth.loginPage(FRONTEND_URL);
                            auth.successHandler(oAuth2Handler);
                            auth.redirectionEndpoint(redirectionEndpoint ->
                                    redirectionEndpoint.baseUri("/oauth2/callback/*"));
                            auth.authorizationEndpoint(authorizationEndpoint ->
                                    authorizationEndpoint.baseUri("/oauth2/authorize")
                            );
                        }
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterAfter(accessTokenFilter, OAuth2LoginAuthenticationFilter.class)
                .addFilterAfter(refreshTokenFilter, AccessTokenFilter.class)
                .logout((out) -> out
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(((request, response, authentication) ->
                                SecurityContextHolder.clearContext()))
                )
                .build();
    }
}

