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
 * Defines three demo users with different named permissions:
 * <ul>
 *   <li><b>admin/admin</b> — All permissions (full CRUD + relation operations)</li>
 *   <li><b>editor/editor</b> — List, Get, Create, Update (no Delete, no Relation REMOVE)</li>
 *   <li><b>viewer/viewer</b> — List and Get only (read-only access)</li>
 * </ul>
 * <p>
 * Permission naming convention: {@code <Action><EntityName>}
 * (e.g., {@code ListProduct}, {@code CreateCustomer}, {@code AddTagToProduct}).
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
                        // Customer CRUD
                        "ListCustomer", "GetCustomer", "CreateCustomer", "UpdateCustomer", "DeleteCustomer",
                        // Customer Profile CRUD
                        "ListCustomerProfile", "GetCustomerProfile", "CreateCustomerProfile",
                        "UpdateCustomerProfile", "DeleteCustomerProfile",
                        // Order CRUD + filter permission
                        "ListOrder", "GetOrder", "CreateOrder", "UpdateOrder", "DeleteOrder",
                        "ListCustomerOrders",
                        // Product CRUD + filter permission
                        "ListProduct", "GetProduct", "CreateProduct", "UpdateProduct", "DeleteProduct",
                        "ListProductsByCategory",
                        // Tag CRUD
                        "ListTag", "GetTag", "CreateTag", "UpdateTag", "DeleteTag",
                        // Product ↔ Tag relation (product side)
                        "ListProductTags", "AddTagToProduct", "RemoveTagFromProduct",
                        // Product ↔ Tag relation (tag side)
                        "ListTaggedProducts", "AssociateProductWithTag", "DetachProductFromTag",
                        // Role
                        "ROLE_ADMIN")
                .build();

        var editor = User.builder()
                .username("editor")
                .password(encoder.encode("editor"))
                .authorities(
                        // Customer — no delete
                        "ListCustomer", "GetCustomer", "CreateCustomer", "UpdateCustomer",
                        // Customer Profile — no delete
                        "ListCustomerProfile", "GetCustomerProfile", "CreateCustomerProfile",
                        "UpdateCustomerProfile",
                        // Order — no delete
                        "ListOrder", "GetOrder", "CreateOrder", "UpdateOrder",
                        "ListCustomerOrders",
                        // Product — no delete
                        "ListProduct", "GetProduct", "CreateProduct", "UpdateProduct",
                        "ListProductsByCategory",
                        // Tag — no delete
                        "ListTag", "GetTag", "CreateTag", "UpdateTag",
                        // Relation — add only, no remove
                        "ListProductTags", "AddTagToProduct",
                        "ListTaggedProducts", "AssociateProductWithTag",
                        // Role
                        "ROLE_EDITOR")
                .build();

        var viewer = User.builder()
                .username("viewer")
                .password(encoder.encode("viewer"))
                .authorities(
                        // Read-only: list and get
                        "ListCustomer", "GetCustomer",
                        "ListCustomerProfile", "GetCustomerProfile",
                        "ListOrder", "GetOrder", "ListCustomerOrders",
                        "ListProduct", "GetProduct", "ListProductsByCategory",
                        "ListTag", "GetTag",
                        // Relation — read only
                        "ListProductTags", "ListTaggedProducts",
                        // Role
                        "ROLE_VIEWER")
                .build();

        return new InMemoryUserDetailsManager(admin, editor, viewer);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
