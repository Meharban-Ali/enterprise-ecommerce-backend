package com.redis.auth.service;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.infrastructure.config.JwtProperties;
import com.redis.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired(required = false)
    private ApiSecurityProperties apiSecurityProperties = new ApiSecurityProperties();

    public JwtService() {}

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.apiSecurityProperties = new ApiSecurityProperties();
    }

    public JwtService(JwtProperties jwtProperties, ApiSecurityProperties apiSecurityProperties) {
        this.jwtProperties = jwtProperties;
        this.apiSecurityProperties = apiSecurityProperties != null ? apiSecurityProperties : new ApiSecurityProperties();
    }

    @PostConstruct
    public void validateSecretKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
            if (keyBytes.length < 32) {
                throw new IllegalStateException("JWT secret key must be at least 256 bits (32 bytes) long to meet cryptographic standards");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalStateException("JWT secret key must be a valid Base64 encoded string", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TOKEN GENERATION
    // ═══════════════════════════════════════════════════════════════════════════

    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole().name());
        
        return buildToken(extraClaims, user.getEmail(), jwtProperties.getExpirationMs());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        if (apiSecurityProperties != null && apiSecurityProperties.isJwtStrictValidationEnabled()) {
            extraClaims.put("iss", "ecommerce-backend");
            extraClaims.put("aud", "ecommerce-clients");
        }

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CLAIM EXTRACTION
    // ═══════════════════════════════════════════════════════════════════════════

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        var parserBuilder = Jwts.parser()
                .verifyWith(getSigningKey());

        if (apiSecurityProperties != null && apiSecurityProperties.isJwtStrictValidationEnabled()) {
            parserBuilder
                .requireIssuer("ecommerce-backend")
                .requireAudience("ecommerce-clients")
                .clockSkewSeconds(60); // 1 minute skew
        }

        return parserBuilder.build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  VALIDATION METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        Claims claims = extractAllClaims(token);

        if (apiSecurityProperties != null && apiSecurityProperties.isJwtStrictValidationEnabled()) {
            Date issuedAt = claims.getIssuedAt();
            if (issuedAt != null) {
                long ageMs = System.currentTimeMillis() - issuedAt.getTime();
                if (ageMs < -60000 || ageMs > jwtProperties.getExpirationMs() + 60000) {
                    return false;
                }
            }
        }

        return (email.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  KEY BUILDER HELPER
    // ═══════════════════════════════════════════════════════════════════════════

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
