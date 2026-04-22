package com.yourorg.gateway.jwt;

import com.yourorg.gateway.model.TokenClaims;

/**
 * Abstraction for JWT validation (REQ-JW-002).
 * Allows swapping JWT library implementations without changing gateway logic.
 */
public interface JwtTokenValidator {

    /**
     * Validates the JWT token and extracts claims.
     *
     * @param token the raw JWT token string
     * @return extracted claims on success
     * @throws com.yourorg.gateway.exception.InvalidAccessTokenException on any validation failure
     */
    TokenClaims validateToken(String token);
}
