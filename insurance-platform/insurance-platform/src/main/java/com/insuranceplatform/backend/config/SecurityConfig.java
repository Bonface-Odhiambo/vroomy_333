package com.insuranceplatform.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.insuranceplatform.backend.enums.UserRole.ADMIN;
import static com.insuranceplatform.backend.enums.UserRole.AGENT;
import static com.insuranceplatform.backend.enums.UserRole.SUPERAGENT;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    // Define the public URLs
    private static final String[] PUBLIC_URLS = {
            "/api/v1/auth/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs/**",
            "/api/v1/payments/mpesa-callback"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless REST APIs
                .authorizeHttpRequests(auth -> auth
                        // Allow public access to authentication, API documentation, and payment callbacks
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        // Role-based access control
                        .requestMatchers("/api/v1/admin/**").hasAuthority(ADMIN.name())
                        .requestMatchers("/api/v1/superagents/**").hasAnyAuthority(ADMIN.name(), SUPERAGENT.name())
                        .requestMatchers("/api/v1/agents/**").hasAnyAuthority(ADMIN.name(), SUPERAGENT.name(), AGENT.name())

                        // All other requests must be authenticated
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // No sessions, JWT is king
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // Add our JWT filter

        return http.build();
    }
}