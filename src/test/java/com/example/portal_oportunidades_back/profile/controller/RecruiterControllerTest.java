package com.example.portal_oportunidades_back.profile.controller;

import com.example.portal_oportunidades_back.exception.GlobalExceptionHandler;
import com.example.portal_oportunidades_back.profile.dto.RecruiterResponse;
import com.example.portal_oportunidades_back.profile.entity.RecruiterType;
import com.example.portal_oportunidades_back.profile.service.RecruiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class RecruiterControllerTest {
    private RecruiterService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(RecruiterService.class);
        mockMvc = standaloneSetup(new RecruiterController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldGetRecruiterProfile() throws Exception {
        when(service.getProfile(10L)).thenReturn(new RecruiterResponse(
                10L, RecruiterType.COMPANY, "Company", true, null, null));

        mockMvc.perform(get("/api/recruiters/10/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.organizationName").value("Company"));
    }

    @Test
    void shouldRejectInvalidProfileUpdate() throws Exception {
        mockMvc.perform(put("/api/recruiters/10/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"COMPANY","organizationName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.organizationName").exists());
    }

    @Test
    void shouldUpdateRecruiterProfile() throws Exception {
        when(service.updateProfile(any(), any())).thenReturn(new RecruiterResponse(
                10L, RecruiterType.PROFESSOR, "Professor", true, null, null));

        mockMvc.perform(put("/api/recruiters/10/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"PROFESSOR","organizationName":"Professor"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PROFESSOR"));
    }
}
