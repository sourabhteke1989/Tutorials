package com.yourorg.usermgmt.controller;

import com.yourorg.usermgmt.service.DataStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class Config {
        @Bean
        public DataStore dataStore() {
            DataStore ds = new DataStore();
            ds.init();
            return ds;
        }
    }

    @Test
    void shouldListUsersByTenant() throws Exception {
        mockMvc.perform(get("/user/list").header("X-Tenant-ID", "acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnEmptyListWhenNoTenantHeader() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldGetCurrentUser() throws Exception {
        mockMvc.perform(get("/user/me").header("X-User-ID", "550e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value("John Admin"));
    }

    @Test
    void shouldReturn400WhenNoUserIdHeader() throws Exception {
        mockMvc.perform(get("/user/me"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForUnknownUser() throws Exception {
        mockMvc.perform(get("/user/me").header("X-User-ID", "nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetUserById() throws Exception {
        mockMvc.perform(get("/user/550e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@acme.com"));
    }

    @Test
    void shouldGetUserByUsername() throws Exception {
        mockMvc.perform(get("/user/by-username")
                        .param("username", "admin@acme.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_name").value("John Admin"));
    }

    @Test
    void shouldGetUserByUsernameWithTenantFilter() throws Exception {
        mockMvc.perform(get("/user/by-username")
                        .param("username", "admin@acme.com")
                        .param("tenant_id", "globex"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateUser() throws Exception {
        mockMvc.perform(post("/user")
                        .contentType("application/json")
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldUpdateUser() throws Exception {
        mockMvc.perform(put("/user/test-id")
                        .contentType("application/json")
                        .content("{\"name\":\"updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/user/test-id"))
                .andExpect(status().isOk());
    }
}
