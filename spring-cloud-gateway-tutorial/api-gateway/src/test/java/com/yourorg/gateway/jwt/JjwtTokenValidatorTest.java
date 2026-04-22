package com.yourorg.gateway.jwt;

import com.yourorg.gateway.config.JwtProperties;
import com.yourorg.gateway.exception.InvalidAccessTokenException;
import com.yourorg.gateway.model.TokenClaims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JjwtTokenValidatorTest {

    private static final String SECRET = "ThisIsAVerySecureSecretKeyForHMACSHA256Algorithm!";
    private static final String ISSUER = "yourorg.com";
    private static final String AUDIENCE = "yourorg.com";

    private JjwtTokenValidator validator;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties("jjwt", SECRET, 900000, ISSUER, AUDIENCE);
        validator = new JjwtTokenValidator(props);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String buildToken(String sub, String tenantId, Date exp) {
        var builder = Jwts.builder()
                .subject(sub)
                .claim("name", "Test User")
                .claim("phone_number", "+1234567890")
                .claim("tenant_id", tenantId)
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .issuedAt(new Date())
                .signWith(key);
        if (exp != null) builder.expiration(exp);
        return builder.compact();
    }

    @Test
    void shouldValidateValidToken() {
        String token = buildToken("user-1", "tenant-1", new Date(System.currentTimeMillis() + 300000));
        TokenClaims claims = validator.validateToken(token);
        assertEquals("user-1", claims.userId());
        assertEquals("Test User", claims.userName());
        assertEquals("+1234567890", claims.phoneNumber());
        assertEquals("tenant-1", claims.tenantId());
    }

    @Test
    void shouldRejectExpiredToken() {
        String token = buildToken("user-1", "tenant-1", new Date(System.currentTimeMillis() - 10000));
        InvalidAccessTokenException ex = assertThrows(InvalidAccessTokenException.class,
                () -> validator.validateToken(token));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void shouldRejectTokenWithWrongKey() {
        SecretKey wrongKey = Keys.hmacShaKeyFor("AnotherSecureKeyThatIsAtLeast32BytesLong!!!!!".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("user-1")
                .claim("tenant_id", "t1")
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .expiration(new Date(System.currentTimeMillis() + 300000))
                .signWith(wrongKey)
                .compact();
        assertThrows(InvalidAccessTokenException.class, () -> validator.validateToken(token));
    }

    @Test
    void shouldRejectTokenWithWrongIssuer() {
        String token = Jwts.builder()
                .subject("user-1")
                .claim("tenant_id", "t1")
                .issuer("wrong-issuer")
                .audience().add(AUDIENCE).and()
                .expiration(new Date(System.currentTimeMillis() + 300000))
                .signWith(key)
                .compact();
        assertThrows(InvalidAccessTokenException.class, () -> validator.validateToken(token));
    }

    @Test
    void shouldRejectTokenWithWrongAudience() {
        String token = Jwts.builder()
                .subject("user-1")
                .claim("tenant_id", "t1")
                .issuer(ISSUER)
                .audience().add("wrong-audience").and()
                .expiration(new Date(System.currentTimeMillis() + 300000))
                .signWith(key)
                .compact();
        assertThrows(InvalidAccessTokenException.class, () -> validator.validateToken(token));
    }

    @Test
    void shouldRejectGarbageToken() {
        assertThrows(InvalidAccessTokenException.class, () -> validator.validateToken("not.a.jwt"));
    }

    @Test
    void shouldRejectEmptyToken() {
        assertThrows(InvalidAccessTokenException.class, () -> validator.validateToken(""));
    }
}
