package com.example.crud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example application demonstrating the CRUD Framework.
 * <p>
 * Entities registered:
 * <ul>
 *   <li>{@code product} → {@code products} table</li>
 *   <li>{@code customer} → {@code customers} table</li>
 * </ul>
 * <p>
 * Uses H2 in-memory database with Spring Security for RBAC demo.
 */
@SpringBootApplication
public class CrudExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrudExampleApplication.class, args);
    }
}
