package com.yourorg.gateway.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvalidAccessTokenExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        InvalidAccessTokenException ex = new InvalidAccessTokenException("test msg");
        assertEquals("test msg", ex.getMessage());
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root");
        InvalidAccessTokenException ex = new InvalidAccessTokenException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
