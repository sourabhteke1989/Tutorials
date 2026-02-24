package com.example.crud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the example application.
 * <p>
 * Defines three demo users with different permission sets:
 * <ul>
 *   <li><b>admin/admin</b> — All permissions (product:*, customer:*)</li>
 *   <li><b>editor/editor</b> — Read + write (no admin/delete)</li>
 *   <li><b>viewer/viewer</b> — Read-only</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/crud/health").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {});

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        var admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin"))
                .authorities(
                        "product:read", "product:write", "product:admin",
                        "customer:read", "customer:write", "customer:admin",
                        "ROLE_ADMIN")
                .build();

        var editor = User.builder()
                .username("editor")
                .password(encoder.encode("editor"))
                .authorities(
                        "product:read", "product:write",
                        "customer:read", "customer:write",
                        "ROLE_EDITOR")
                .build();

        var viewer = User.builder()
                .username("viewer")
                .password(encoder.encode("viewer"))
                .authorities(
                        "product:read",
                        "customer:read",
                        "ROLE_VIEWER")
                .build();

        return new InMemoryUserDetailsManager(admin, editor, viewer);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
