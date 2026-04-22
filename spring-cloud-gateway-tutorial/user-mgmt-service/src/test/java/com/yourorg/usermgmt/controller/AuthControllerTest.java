package com.yourorg.usermgmt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.yourorg.usermgmt.model.LoginRequest;
import com.yourorg.usermgmt.service.DataStore;
import com.yourorg.usermgmt.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

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

        @Bean
        public JwtService jwtService() {
            return new JwtService(
                    "ThisIsAVerySecureSecretKeyForHMACSHA256Algorithm!",
                    900000, "yourorg.com", "yourorg.com");
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldInitiate() throws Exception {
        mockMvc.perform(get("/auth/internal-auth/initiate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth_mode").value("internal"));
    }

    @Test
    void shouldIdentifyExistingUser() throws Exception {
        mockMvc.perform(post("/auth/internal-auth/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@acme.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identified").value(true));
    }

    @Test
    void shouldNotIdentifyUnknownUser() throws Exception {
        mockMvc.perform(post("/auth/internal-auth/identify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identified").value(false));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest req = new LoginRequest("admin@acme.com", "admin123", "acme");
        mockMvc.perform(post("/auth/internal-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    void shouldRejectInvalidPassword() throws Exception {
        LoginRequest req = new LoginRequest("admin@acme.com", "wrong", "acme");
        mockMvc.perform(post("/auth/internal-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnknownUser() throws Exception {
        LoginRequest req = new LoginRequest("nobody@test.com", "pass", "acme");
        mockMvc.perform(post("/auth/internal-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectTenantMismatch() throws Exception {
        LoginRequest req = new LoginRequest("admin@acme.com", "admin123", "globex");
        mockMvc.perform(post("/auth/internal-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRefreshToken() throws Exception {
        mockMvc.perform(post("/auth/internal-auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetLoggedInUsers() throws Exception {
        mockMvc.perform(get("/auth/internal-auth/logged-in-users"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetPublicKey() throws Exception {
        mockMvc.perform(get("/auth/internal-auth/public-key"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldLogout() throws Exception {
        mockMvc.perform(post("/auth/internal-auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldLogoutAll() throws Exception {
        mockMvc.perform(post("/auth/internal-auth/logout-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
