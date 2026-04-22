package com.yourorg.usermgmt.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MiscController.class)
class MiscControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnApplications() throws Exception {
        mockMvc.perform(get("/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void shouldReturnPermissionCategories() throws Exception {
        mockMvc.perform(get("/permission-categories"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnPermissions() throws Exception {
        mockMvc.perform(get("/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void shouldReturnResourceTypes() throws Exception {
        mockMvc.perform(get("/application/user-mgmt/resource-types"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnVersion() throws Exception {
        mockMvc.perform(get("/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void shouldReturnAppPermissions() throws Exception {
        mockMvc.perform(get("/user-access-control/app-permissions"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnResourceIds() throws Exception {
        mockMvc.perform(get("/user-access-control/resource-ids")
                        .param("user_id", "u1")
                        .param("app_id", "a1")
                        .param("resource_type", "doc")
                        .param("permission", "READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnHasAccess() throws Exception {
        mockMvc.perform(get("/user-access-control/has-access")
                        .param("user_id", "u1")
                        .param("resource_id", "r1")
                        .param("permission", "READ"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnOrgGroups() throws Exception {
        mockMvc.perform(get("/org-group/list"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldCreateOrgGroup() throws Exception {
        mockMvc.perform(post("/org-group")
                        .contentType("application/json")
                        .content("{\"name\":\"new\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetOrgGroup() throws Exception {
        mockMvc.perform(get("/org-group/group-1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldUpdateOrgGroup() throws Exception {
        mockMvc.perform(put("/org-group/group-1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteOrgGroup() throws Exception {
        mockMvc.perform(delete("/org-group/group-1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnOrgPolicies() throws Exception {
        mockMvc.perform(get("/org-policy/list"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldCreateOrgPolicy() throws Exception {
        mockMvc.perform(post("/org-policy")
                        .contentType("application/json")
                        .content("{\"name\":\"policy\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetOrgPolicy() throws Exception {
        mockMvc.perform(get("/org-policy/policy-1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteOrgPolicy() throws Exception {
        mockMvc.perform(delete("/org-policy/policy-1"))
                .andExpect(status().isOk());
    }
}
