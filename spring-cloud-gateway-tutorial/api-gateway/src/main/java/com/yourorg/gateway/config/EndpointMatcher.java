package com.yourorg.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Precompiles endpoint URL patterns into regex at startup (REQ-AU-020, REQ-AU-022).
 * Provides method-aware endpoint matching (REQ-AU-021).
 */
@Component
public class EndpointMatcher {

    private static final Logger log = LoggerFactory.getLogger(EndpointMatcher.class);

    private final Map<String, List<CompiledEndpoint>> publicEndpoints;
    private final Map<String, List<CompiledEndpoint>> privateEndpoints;

    public EndpointMatcher(ConfigLoader configLoader) {
        SecurityConfig securityConfig = configLoader.getSecurityConfig();
        this.publicEndpoints = new HashMap<>();
        this.privateEndpoints = new HashMap<>();

        for (Map.Entry<String, SecurityConfig.ServiceSecurityConfig> entry : securityConfig.services().entrySet()) {
            String serviceKey = entry.getKey();
            SecurityConfig.ServiceSecurityConfig serviceConfig = entry.getValue();

            publicEndpoints.put(serviceKey, compileEndpoints(serviceConfig.publicEndpoints(), serviceKey, "public"));
            privateEndpoints.put(serviceKey, compileEndpoints(serviceConfig.privateEndpoints(), serviceKey, "private"));
        }

        log.info("Compiled endpoint patterns for {} services", publicEndpoints.size());
    }

    private List<CompiledEndpoint> compileEndpoints(List<SecurityConfig.EndpointConfig> endpoints,
                                                     String serviceKey, String type) {
        if (endpoints == null) {
            return List.of();
        }

        List<CompiledEndpoint> compiled = new ArrayList<>();
        for (SecurityConfig.EndpointConfig ep : endpoints) {
            String regex = convertToRegex(ep.url());
            Pattern pattern = Pattern.compile(regex);
            compiled.add(new CompiledEndpoint(ep.url(), ep.method().toUpperCase(), pattern));
            log.debug("Compiled {} endpoint for service {}: {} {} -> {}", type, serviceKey, ep.method(), ep.url(), regex);
        }
        return Collections.unmodifiableList(compiled);
    }

    /**
     * Converts a URL template with {paramName} placeholders into a regex pattern.
     * {paramName} → matches one or more characters excluding /
     */
    static String convertToRegex(String urlTemplate) {
        String regex = urlTemplate.replaceAll("\\{[^}]+}", "[^/]+");
        return "^" + regex + "$";
    }

    public boolean isPublicEndpoint(String serviceKey, String path, String method) {
        return matchesEndpoint(publicEndpoints.getOrDefault(serviceKey, List.of()), path, method);
    }

    public boolean isPrivateEndpoint(String serviceKey, String path, String method) {
        return matchesEndpoint(privateEndpoints.getOrDefault(serviceKey, List.of()), path, method);
    }

    public boolean isServiceKnown(String serviceKey) {
        return publicEndpoints.containsKey(serviceKey) || privateEndpoints.containsKey(serviceKey);
    }

    private boolean matchesEndpoint(List<CompiledEndpoint> endpoints, String path, String method) {
        for (CompiledEndpoint ep : endpoints) {
            if (ep.method().equals(method.toUpperCase()) && ep.pattern().matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    public record CompiledEndpoint(String urlTemplate, String method, Pattern pattern) {
    }
}
