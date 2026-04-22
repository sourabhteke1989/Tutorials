package com.yourorg.gateway.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.yourorg.gateway.config.JwtProperties;
import com.yourorg.gateway.exception.InvalidAccessTokenException;
import com.yourorg.gateway.model.TokenClaims;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Nimbus JOSE+JWT-based validation (REQ-JW-002, REQ-JW-003).
 */
public class NimbusJwtTokenValidator implements JwtTokenValidator {

    private final JWSVerifier verifier;
    private final String issuer;
    private final String audience;

    public NimbusJwtTokenValidator(JwtProperties properties) {
        try {
            this.verifier = new MACVerifier(properties.secretKey().getBytes(StandardCharsets.UTF_8));
        } catch (JOSEException e) {
            throw new IllegalArgumentException("Invalid JWT secret key for Nimbus: " + e.getMessage(), e);
        }
        this.issuer = properties.issuer();
        this.audience = properties.audience();
    }

    @Override
    public TokenClaims validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                throw new InvalidAccessTokenException("Token signature verification failed");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Check expiration
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                throw new InvalidAccessTokenException("Token has expired");
            }

            // Check not-before
            Date notBeforeTime = claims.getNotBeforeTime();
            if (notBeforeTime != null && notBeforeTime.after(new Date())) {
                throw new InvalidAccessTokenException("Token is not yet valid");
            }

            // Check issuer
            if (!issuer.equals(claims.getIssuer())) {
                throw new InvalidAccessTokenException("Invalid token issuer");
            }

            // Check audience
            List<String> aud = claims.getAudience();
            if (aud == null || !aud.contains(audience)) {
                throw new InvalidAccessTokenException("Invalid token audience");
            }

            String userId = claims.getSubject();
            String userName = (String) claims.getClaim("name");
            String phoneNumber = (String) claims.getClaim("phone_number");
            String tenantId = (String) claims.getClaim("tenant_id");

            return new TokenClaims(userId, userName, phoneNumber, tenantId);
        } catch (ParseException e) {
            throw new InvalidAccessTokenException("Failed to parse JWT token");
        } catch (JOSEException e) {
            throw new InvalidAccessTokenException("Token verification error: " + e.getMessage());
        }
    }
}
