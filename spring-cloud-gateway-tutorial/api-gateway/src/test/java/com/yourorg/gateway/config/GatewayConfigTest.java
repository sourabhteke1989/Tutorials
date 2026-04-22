package com.yourorg.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GatewayConfigTest {

    @Test
    void objectMapperUsesSnakeCaseNaming() {
        GatewayConfig gatewayConfig = new GatewayConfig();
        ObjectMapper mapper = gatewayConfig.objectMapper();

        assertNotNull(mapper);
        assertEquals(PropertyNamingStrategies.SNAKE_CASE, mapper.getPropertyNamingStrategy());
    }

    @Test
    void objectMapperCanSerializeAndDeserialize() throws Exception {
        GatewayConfig gatewayConfig = new GatewayConfig();
        ObjectMapper mapper = gatewayConfig.objectMapper();

        // Test that the mapper works with snake_case
        TestData data = new TestData("hello", 42);
        String json = mapper.writeValueAsString(data);
        assertTrue(json.contains("my_name"));
        assertTrue(json.contains("my_value"));

        TestData deserialized = mapper.readValue(json, TestData.class);
        assertEquals("hello", deserialized.myName);
        assertEquals(42, deserialized.myValue);
    }

    static class TestData {
        public String myName;
        public int myValue;

        public TestData() {}

        public TestData(String myName, int myValue) {
            this.myName = myName;
            this.myValue = myValue;
        }
    }
}
