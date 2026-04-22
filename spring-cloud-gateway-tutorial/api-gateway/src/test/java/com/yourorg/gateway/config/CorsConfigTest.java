package com.yourorg.gateway.config;

import com.yourorg.gateway.config.GatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    void shouldCreateCorsFilterInDevelopmentMode() {
        GatewayProperties props = new GatewayProperties(
                "r.json", "s.json",
                true, // developmentMode
                false, "production", null, null);

        CorsConfig corsConfig = new CorsConfig();
        CorsWebFilter filter = corsConfig.corsWebFilter(props);

        assertNotNull(filter);
    }

    @Test
    void shouldCreateCorsFilterInProductionMode() {
        GatewayProperties props = new GatewayProperties(
                "r.json", "s.json",
                false, // not developmentMode
                false, "production", null, null);

        CorsConfig corsConfig = new CorsConfig();
        CorsWebFilter filter = corsConfig.corsWebFilter(props);

        assertNotNull(filter);
    }
}
