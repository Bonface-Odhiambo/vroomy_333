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

import static com.insuranceplatform.backend.enums.UserRole.*; // Import all roles

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final ApiKeyAuthFilter apiKeyAuthFilter; // Inject the new filter
    private final AuthenticationProvider authenticationProvider;

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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                // Add our filters. The ApiKey filter runs before the JWT filter.
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        // --- NEW SECURITY RULE FOR THE DATA API ---
                        .requestMatchers("/api/v1/data-sharing/**").hasRole("API_USER")

                        // Role-based access control for platform users
                        .requestMatchers("/api/v1/admin/**").hasAuthority(ADMIN.name())
                        .requestMatchers("/api/v1/superagents/**").hasAnyAuthority(ADMIN.name(), SUPERAGENT.name())
                        .requestMatchers("/api/v1/agents/**").hasAnyAuthority(ADMIN.name(), SUPERAGENT.name(), AGENT.name())
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}