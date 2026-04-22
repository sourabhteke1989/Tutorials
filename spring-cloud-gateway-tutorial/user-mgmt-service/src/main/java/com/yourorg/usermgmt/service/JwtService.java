package com.yourorg.usermgmt.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiration;
    private final String issuer;
    private final String audience;

    public JwtService(@Value("${jwt.secret-key}") String secret,
                      @Value("${jwt.expiration}") long expiration,
                      @Value("${jwt.issuer}") String issuer,
                      @Value("${jwt.audience}") String audience) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generateToken(String userId, String userName, String phoneNumber, String tenantId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(now)
                .expiration(expiryDate)
                .claims(Map.of(
                        "name", userName,
                        "phone_number", phoneNumber,
                        "tenant_id", tenantId
                ))
                .signWith(secretKey)
                .compact();
    }
}
