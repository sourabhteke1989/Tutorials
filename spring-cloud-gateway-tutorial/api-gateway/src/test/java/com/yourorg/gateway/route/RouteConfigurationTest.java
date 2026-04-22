package com.yourorg.gateway.route;

import com.yourorg.gateway.config.ConfigLoader;
import com.yourorg.gateway.config.GatewayProperties;
import com.yourorg.gateway.config.RouterConfig;
import com.yourorg.gateway.filter.AuthenticationFilterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.*;

class RouteConfigurationTest {

    private RouteConfiguration routeConfiguration;

    @BeforeEach
    void setUp() {
        routeConfiguration = new RouteConfiguration();
    }

    @Test
    void buildTenantHostPattern_singleHost() throws Exception {
        RouterConfig config = new RouterConfig(
                List.of("yourorg.com"), null, null, Map.of(), Map.of());

        Method method = RouteConfiguration.class.getDeclaredMethod("buildTenantHostPattern", RouterConfig.class);
        method.setAccessible(true);
        String[] result = (String[]) method.invoke(routeConfiguration, config);

        assertArrayEquals(new String[]{"**.yourorg.com"}, result);
    }

    @Test
    void buildTenantHostPattern_multipleHosts() throws Exception {
        RouterConfig config = new RouterConfig(
                List.of("yourorg.com", "yourorg.cloud"), null, null, Map.of(), Map.of());

        Method method = RouteConfiguration.class.getDeclaredMethod("buildTenantHostPattern", RouterConfig.class);
        method.setAccessible(true);
        String[] result = (String[]) method.invoke(routeConfiguration, config);

        assertArrayEquals(new String[]{"**.yourorg.com", "**.yourorg.cloud"}, result);
    }

    @Test
    void buildBareHostPattern_singleHost() throws Exception {
        RouterConfig config = new RouterConfig(
                List.of("yourorg.com"), null, null, Map.of(), Map.of());

        Method method = RouteConfiguration.class.getDeclaredMethod("buildBareHostPattern", RouterConfig.class);
        method.setAccessible(true);
        String[] result = (String[]) method.invoke(routeConfiguration, config);

        assertArrayEquals(new String[]{"yourorg.com"}, result);
    }

    @Test
    void buildBareHostPattern_multipleHosts() throws Exception {
        RouterConfig config = new RouterConfig(
                List.of("yourorg.com", "yourorg.cloud"), null, null, Map.of(), Map.of());

        Method method = RouteConfiguration.class.getDeclaredMethod("buildBareHostPattern", RouterConfig.class);
        method.setAccessible(true);
        String[] result = (String[]) method.invoke(routeConfiguration, config);

        assertArrayEquals(new String[]{"yourorg.com", "yourorg.cloud"}, result);
    }

    @Test
    void customRouteLocator_createsRoutesWithMocks() {
        GatewayProperties props = new GatewayProperties(
                "router-config-monolith.json",
                "security-config.json",
                true, false, "production", null, null);
        ConfigLoader configLoader = new ConfigLoader(props);

        RouteLocatorBuilder builder = mock(RouteLocatorBuilder.class, RETURNS_DEEP_STUBS);
        AuthenticationFilterFactory authFilterFactory = mock(AuthenticationFilterFactory.class);
        when(authFilterFactory.apply(any(AuthenticationFilterFactory.Config.class))).thenReturn((exchange, chain) -> chain.filter(exchange));

        RouteLocatorBuilder.Builder routesBuilder = mock(RouteLocatorBuilder.Builder.class, RETURNS_DEEP_STUBS);
        when(builder.routes()).thenReturn(routesBuilder);
        when(routesBuilder.route(anyString(), any())).thenReturn(routesBuilder);

        RouteLocator locator = mock(RouteLocator.class);
        when(routesBuilder.build()).thenReturn(locator);

        RouteLocator result = routeConfiguration.customRouteLocator(builder, configLoader, props, authFilterFactory);
        assertNotNull(result);

        int expectedRoutes = configLoader.getRouterConfig().services().size() * 2
                + configLoader.getRouterConfig().uiApplications().size()
                + 1 // landing
                + 1; // website catchall
        verify(routesBuilder, atLeast(expectedRoutes)).route(anyString(), any());
    }

    @Test
    void customRouteLocator_noLandingUrl() {
        RouterConfig routerConfig = new RouterConfig(
                List.of("yourorg.com"), null, null,
                Map.of("user-mgmt", "http://localhost:8081"),
                Map.of()
        );
        ConfigLoader configLoader = mock(ConfigLoader.class);
        when(configLoader.getRouterConfig()).thenReturn(routerConfig);

        GatewayProperties props = new GatewayProperties(
                "r.json", "s.json", true, false, "production", null, null);

        RouteLocatorBuilder builder = mock(RouteLocatorBuilder.class, RETURNS_DEEP_STUBS);
        AuthenticationFilterFactory authFilterFactory = mock(AuthenticationFilterFactory.class);
        when(authFilterFactory.apply(any(AuthenticationFilterFactory.Config.class))).thenReturn((exchange, chain) -> chain.filter(exchange));

        RouteLocatorBuilder.Builder routesBuilder = mock(RouteLocatorBuilder.Builder.class, RETURNS_DEEP_STUBS);
        when(builder.routes()).thenReturn(routesBuilder);
        when(routesBuilder.route(anyString(), any())).thenReturn(routesBuilder);
        when(routesBuilder.build()).thenReturn(mock(RouteLocator.class));

        RouteLocator result = routeConfiguration.customRouteLocator(builder, configLoader, props, authFilterFactory);
        assertNotNull(result);

        // Only 2 routes (tenant-api and website-api for user-mgmt), no landing, no catchall
        verify(routesBuilder, times(2)).route(anyString(), any());
    }

    @Test
    void customRouteLocator_withWebsiteUrl() {
        RouterConfig routerConfig = new RouterConfig(
                List.of("yourorg.com"), "http://website.com", null,
                Map.of("user-mgmt", "http://localhost:8081"),
                Map.of()
        );
        ConfigLoader configLoader = mock(ConfigLoader.class);
        when(configLoader.getRouterConfig()).thenReturn(routerConfig);

        GatewayProperties props = new GatewayProperties(
                "r.json", "s.json", true, false, "production", null, null);

        RouteLocatorBuilder builder = mock(RouteLocatorBuilder.class, RETURNS_DEEP_STUBS);
        AuthenticationFilterFactory authFilterFactory = mock(AuthenticationFilterFactory.class);
        when(authFilterFactory.apply(any(AuthenticationFilterFactory.Config.class))).thenReturn((exchange, chain) -> chain.filter(exchange));

        RouteLocatorBuilder.Builder routesBuilder = mock(RouteLocatorBuilder.Builder.class, RETURNS_DEEP_STUBS);
        when(builder.routes()).thenReturn(routesBuilder);
        when(routesBuilder.route(anyString(), any())).thenReturn(routesBuilder);
        when(routesBuilder.build()).thenReturn(mock(RouteLocator.class));

        RouteLocator result = routeConfiguration.customRouteLocator(builder, configLoader, props, authFilterFactory);
        assertNotNull(result);

        // 2 service routes + website catchall = 3
        verify(routesBuilder, times(3)).route(anyString(), any());
    }

    @Test
    void customRouteLocator_withUiApplications() {
        RouterConfig routerConfig = new RouterConfig(
                List.of("yourorg.com"), null, "http://localhost:3000",
                Map.of("user-mgmt", "http://localhost:8081"),
                Map.of("dashboard", "http://localhost:3000", "settings", "http://localhost:3001")
        );
        ConfigLoader configLoader = mock(ConfigLoader.class);
        when(configLoader.getRouterConfig()).thenReturn(routerConfig);

        GatewayProperties props = new GatewayProperties(
                "r.json", "s.json", true, false, "staging", null, null);

        RouteLocatorBuilder builder = mock(RouteLocatorBuilder.class, RETURNS_DEEP_STUBS);
        AuthenticationFilterFactory authFilterFactory = mock(AuthenticationFilterFactory.class);
        when(authFilterFactory.apply(any(AuthenticationFilterFactory.Config.class))).thenReturn((exchange, chain) -> chain.filter(exchange));

        RouteLocatorBuilder.Builder routesBuilder = mock(RouteLocatorBuilder.Builder.class, RETURNS_DEEP_STUBS);
        when(builder.routes()).thenReturn(routesBuilder);
        when(routesBuilder.route(anyString(), any())).thenReturn(routesBuilder);
        when(routesBuilder.build()).thenReturn(mock(RouteLocator.class));

        RouteLocator result = routeConfiguration.customRouteLocator(builder, configLoader, props, authFilterFactory);
        assertNotNull(result);

        // 2 service routes + 2 UI routes + 1 landing = 5
        verify(routesBuilder, times(5)).route(anyString(), any());
    }

    @Test
    void customRouteLocator_allRouteTypes() {
        RouterConfig routerConfig = new RouterConfig(
                List.of("yourorg.com"), "http://website.com", "http://landing.com",
                Map.of("svc-a", "http://localhost:8081", "svc-b", "http://localhost:8082"),
                Map.of("app-a", "http://localhost:3000")
        );
        ConfigLoader configLoader = mock(ConfigLoader.class);
        when(configLoader.getRouterConfig()).thenReturn(routerConfig);

        GatewayProperties props = new GatewayProperties(
                "r.json", "s.json", true, false, "production", null, null);

        RouteLocatorBuilder builder = mock(RouteLocatorBuilder.class, RETURNS_DEEP_STUBS);
        AuthenticationFilterFactory authFilterFactory = mock(AuthenticationFilterFactory.class);
        when(authFilterFactory.apply(any(AuthenticationFilterFactory.Config.class))).thenReturn((exchange, chain) -> chain.filter(exchange));

        RouteLocatorBuilder.Builder routesBuilder = mock(RouteLocatorBuilder.Builder.class, RETURNS_DEEP_STUBS);
        when(builder.routes()).thenReturn(routesBuilder);
        when(routesBuilder.route(anyString(), any())).thenReturn(routesBuilder);
        when(routesBuilder.build()).thenReturn(mock(RouteLocator.class));

        RouteLocator result = routeConfiguration.customRouteLocator(builder, configLoader, props, authFilterFactory);
        assertNotNull(result);

        // 4 service routes (2 tenant + 2 website) + 1 UI route + 1 landing + 1 website catchall = 7
        verify(routesBuilder, times(7)).route(anyString(), any());
    }
}
