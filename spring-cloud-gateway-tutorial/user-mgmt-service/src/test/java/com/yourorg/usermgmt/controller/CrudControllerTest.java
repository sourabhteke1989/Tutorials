package com.yourorg.usermgmt.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CrudController.class)
class CrudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnHealth() throws Exception {
        mockMvc.perform(get("/crud/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldReturnEntities() throws Exception {
        mockMvc.perform(get("/crud/entities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void shouldHandleCrud() throws Exception {
        mockMvc.perform(post("/crud")
                        .contentType("application/json")
                        .content("{\"entity\":\"user\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandleCrudRelation() throws Exception {
        mockMvc.perform(post("/crud/relation")
                        .contentType("application/json")
                        .content("{\"from\":\"user\",\"to\":\"group\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandleFileUpload() throws Exception {
        mockMvc.perform(post("/crud/file-upload-request")
                        .contentType("application/json")
                        .content("{\"filename\":\"test.txt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upload_token").isNotEmpty());
    }

    @Test
    void shouldHandleFileDownload() throws Exception {
        mockMvc.perform(post("/crud/file-download-request")
                        .contentType("application/json")
                        .content("{\"filename\":\"test.txt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.download_token").isNotEmpty());
    }

    @Test
    void shouldHandlePublicCrud() throws Exception {
        mockMvc.perform(post("/crud/public")
                        .contentType("application/json")
                        .content("{\"data\":\"test\"}"))
                .andExpect(status().isOk());
    }
}
