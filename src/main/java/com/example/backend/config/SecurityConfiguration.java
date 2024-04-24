package com.example.backend.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final LogoutHandler logoutHandler;

    // we can choose what url we want to secure
    // for example white list: url without auth
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // disable verification

                .authorizeHttpRequests((request) -> {
                            request
                                    .requestMatchers("/auth/**") // white list
                                    .permitAll()
                                    .anyRequest()  // any other must be auth
                                    .authenticated();
                        }
                )
                .sessionManagement((session) -> {
                            // new session for each request
                            session
                                    .sessionCreationPolicy(
                                            SessionCreationPolicy.STATELESS);
                        }
                )
                // which provider we use
                .authenticationProvider(authenticationProvider)
                // use jwt filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // add logout
                .logout((out) -> out
                        .logoutUrl("/logout") // logout endpoint
                        .addLogoutHandler(logoutHandler)
                        // when logout success
                        .logoutSuccessHandler(((request, response, authentication) ->
                                SecurityContextHolder.clearContext()))
                );
        return http.build();
    }
}

