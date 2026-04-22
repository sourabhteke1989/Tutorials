package com.yourorg.gateway.jwt;

import com.yourorg.gateway.config.JwtProperties;
import com.yourorg.gateway.exception.InvalidAccessTokenException;
import com.yourorg.gateway.model.TokenClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JJWT-based JWT validation (REQ-JW-002, REQ-JW-003).
 */
public class JjwtTokenValidator implements JwtTokenValidator {

    private final SecretKey secretKey;
    private final String issuer;
    private final String audience;

    public JjwtTokenValidator(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secretKey().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.issuer();
        this.audience = properties.audience();
    }

    @Override
    public TokenClaims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String userName = claims.get("name", String.class);
            String phoneNumber = claims.get("phone_number", String.class);
            String tenantId = claims.get("tenant_id", String.class);

            return new TokenClaims(userId, userName, phoneNumber, tenantId);
        } catch (ExpiredJwtException e) {
            throw new InvalidAccessTokenException("Token has expired");
        } catch (JwtException e) {
            throw new InvalidAccessTokenException("Invalid token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new InvalidAccessTokenException("Token claims are invalid: " + e.getMessage());
        }
    }
}
