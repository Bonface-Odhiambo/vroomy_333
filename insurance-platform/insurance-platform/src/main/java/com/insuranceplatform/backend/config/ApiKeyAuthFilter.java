package com.insuranceplatform.backend.config;

import com.insuranceplatform.backend.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull; // Import Lombok's @NonNull annotation
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    public static final String API_KEY_HEADER = "X-API-KEY";

    /**
     * This filter intercepts requests to check for a valid API key in the header.
     * If a valid key is found, it authenticates the request with a specific "API_USER" role.
     * The @NonNull annotations are added to satisfy the contract of the parent class.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String apiKeyHeaderValue = request.getHeader(API_KEY_HEADER);

        if (apiKeyHeaderValue != null) {
            apiKeyRepository.findByKeyValue(apiKeyHeaderValue).ifPresent(apiKey -> {
                if (apiKey.isEnabled()) {
                    // Key is valid, create an authentication token
                    var auth = new UsernamePasswordAuthenticationToken(
                            apiKey.getInsuranceCompany().getName(), // Principal is the company name
                            null, // No credentials needed for API key auth
                            AuthorityUtils.createAuthorityList("ROLE_API_USER") // Grant a specific role
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    // Update the last used timestamp for auditing purposes
                    apiKey.setLastUsed(LocalDateTime.now());
                    apiKeyRepository.save(apiKey);
                }
            });
        }
        
        // Continue the filter chain for other authentication mechanisms (like JWT) to run.
        filterChain.doFilter(request, response);
    }
}