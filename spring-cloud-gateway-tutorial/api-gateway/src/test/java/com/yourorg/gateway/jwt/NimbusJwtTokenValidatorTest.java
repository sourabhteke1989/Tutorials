package com.yourorg.gateway.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.yourorg.gateway.config.JwtProperties;
import com.yourorg.gateway.exception.InvalidAccessTokenException;
import com.yourorg.gateway.model.TokenClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class NimbusJwtTokenValidatorTest {

    private static final String SECRET = "ThisIsAVerySecureSecretKeyForHMACSHA256Algorithm!";
    private static final String ISSUER = "yourorg.com";
    private static final String AUDIENCE = "yourorg.com";

    private NimbusJwtTokenValidator validator;
    private MACSigner signer;

    @BeforeEach
    void setUp() throws JOSEException {
        JwtProperties props = new JwtProperties("nimbus", SECRET, 900000, ISSUER, AUDIENCE);
        validator = new NimbusJwtTokenValidator(props);
        signer = new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String buildToken(String sub, String tenantId, Date exp, Date nbf) throws JOSEException {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("name", "Test User")
                .claim("phone_number", "+1234567890")
                .claim("tenant_id", tenantId)
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .issueTime(new Date());
        if (exp != null) builder.expirationTime(exp);
        if (nbf != null) builder.notBeforeTime(nbf);

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    @Test
    void shouldValidateValidToken() throws JOSEException {
        String token = buildToken("user-1", "tenant-1", new Date(System.currentTimeMillis() + 300000), null);
        TokenClaims claims = validator.validateToken(token);
        assertEquals("user-1", claims.userId());
        assertEquals("Test User", claims.userName());
        assertEquals("tenant-1", claims.tenantId());
    }

    @Test
    void shouldRejectExpiredToken() throws JOSEException {
        String token = buildToken("user-1", "tenant-1", new Date(System.currentTimeMillis() - 10000), null);
        InvalidAccessTokenException ex = assertThrows(InvalidAccessTokenException.class,
                () -> validator.validateToken(token));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void shouldRejectNotYetValidToken() throws JOSEException {
        String token = buildToken("user-1", "tenant-1",
                new Date(System.currentTimeMillis() + 300000),
                new Date(System.currentTimeMillis() + 60000));
        InvalidAccessTokenException ex = assertThrows(InvalidAccessTokenException.class,
                () -> validator.validateToken(token));
        assertTrue(ex.getMessage().contains("not yet valid"));
    }

    @Test
    void shouldRejectTokenWithWrongSignature() throws JOSEException {
        MACSigner wrongSigner = new MACSigner("AnotherSecureKeyThatIsAtLeast32BytesLong!!!!!".getBytes(StandardCharsets.UTF_8));
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("user-1")
                .claim("tenant_id", "t1")
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .expirationTime(new Date(System.currentTimeMillis() + 300000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(wrongSigner);
        assertThrows(InvalidAccessTokenException.class, () -> validator.validateToken(jwt.serialize()));
    }

    @Test
    void shouldRejectTokenWithWrongIssuer() throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("user-1")
                .claim("tenant_id", "t1")
                .issuer("wrong-issuer")
                .audience(AUDIENCE)
                .expirationTime(new Date(System.currentTimeMillis() + 300000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(signer);
        assertThrows(InvalidAccessTokenException.class, () -> validator.validateToken(jwt.serialize()));
    }

    @Test
    void shouldRejectTokenWithWrongAudience() throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("user-1")
                .claim("tenant_id", "t1")
                .issuer(ISSUER)
                .audience("wrong-aud")
                .expirationTime(new Date(System.currentTimeMillis() + 300000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(signer);
        assertThrows(InvalidAccessTokenException.class, () -> validator.validateToken(jwt.serialize()));
    }

    @Test
    void shouldRejectGarbageToken() {
        assertThrows(InvalidAccessTokenException.class, () -> validator.validateToken("not.a.jwt"));
    }
}
