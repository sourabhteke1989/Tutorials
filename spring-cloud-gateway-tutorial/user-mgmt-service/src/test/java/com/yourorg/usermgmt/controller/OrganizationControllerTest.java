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

@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {

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
    void shouldGetOrganizationDetails() throws Exception {
        mockMvc.perform(get("/organization/details").param("tenant_id", "acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Corporation"));
    }

    @Test
    void shouldReturn404ForUnknownTenant() throws Exception {
        mockMvc.perform(get("/organization/details").param("tenant_id", "nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenNoTenantId() throws Exception {
        mockMvc.perform(get("/organization/details"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCheckTenantExists() throws Exception {
        mockMvc.perform(get("/organization/exists/acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void shouldCheckTenantNotExists() throws Exception {
        mockMvc.perform(get("/organization/exists/unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    void shouldRegisterOrganization() throws Exception {
        mockMvc.perform(post("/organization/register")
                        .contentType("application/json")
                        .content("{\"name\":\"New Org\"}"))
                .andExpect(status().isOk());
    }
}
