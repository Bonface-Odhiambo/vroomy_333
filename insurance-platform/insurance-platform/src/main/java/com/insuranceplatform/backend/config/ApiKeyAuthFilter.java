package com.insuranceplatform.backend.config;

import com.insuranceplatform.backend.entity.ApiKey;
import com.insuranceplatform.backend.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKeyHeaderValue = request.getHeader(API_KEY_HEADER);

        if (apiKeyHeaderValue != null) {
            apiKeyRepository.findByKeyValue(apiKeyHeaderValue).ifPresent(apiKey -> {
                if (apiKey.isEnabled()) {
                    // Key is valid, grant authentication with a specific role
                    var auth = new UsernamePasswordAuthenticationToken(
                            apiKey.getInsuranceCompany().getName(), // The principal is the company name
                            null,
                            AuthorityUtils.createAuthorityList("ROLE_API_USER") // A special role for API access
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    // Update last used timestamp
                    apiKey.setLastUsed(LocalDateTime.now());
                    apiKeyRepository.save(apiKey);
                }
            });
        }
        filterChain.doFilter(request, response);
    }
}