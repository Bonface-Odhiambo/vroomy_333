package com.insuranceplatform.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret.key}")
    private String SECRET_KEY;

    @Value("${jwt.expiration.ms}")
    private long jwtExpiration;

    // Injected property for the refresh token's longer lifespan
    @Value("${jwt.refresh-expiration.ms}")
    private long refreshExpiration;

    // --- Public API Methods ---

    /**
     * Extracts the username (email) from the JWT token.
     * @param token The JWT token.
     * @return The username (subject) of the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generates a standard access token for a user.
     * These tokens have a shorter lifespan.
     * @param userDetails The user details object.
     * @return A signed JWT access token.
     */
    public String generateToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, jwtExpiration);
    }

    /**
     * Generates a refresh token for a user.
     * These tokens have a much longer lifespan and are used to obtain new access tokens.
     * @param userDetails The user details object.
     * @return A signed JWT refresh token.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    /**
     * Validates if a token is valid for a given user.
     * Checks if the username matches and if the token has not expired.
     * @param token The JWT token to validate.
     * @param userDetails The user to validate against.
     * @return true if the token is valid, false otherwise.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // --- Private Helper Methods ---

    /**
     * A generic method to extract a specific claim from a token.
     * @param token The JWT token.
     * @param claimsResolver A function to apply to the claims.
     * @return The resolved claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * The core builder method for creating both access and refresh tokens.
     * @param extraClaims Additional claims to add to the token.
     * @param userDetails The user for whom the token is being created.
     * @param expiration The expiration time in milliseconds for this token.
     * @return A compact, signed JWT string.
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}