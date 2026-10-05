package com.example.portal_oportunidades_back.opportunity.controller;

import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.exception.GlobalExceptionHandler;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.service.OpportunityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class OpportunityControllerTest {
    private OpportunityService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(OpportunityService.class);
        mockMvc = standaloneSetup(new OpportunityController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateDraftOpportunity() throws Exception {
        when(service.create(any(), any())).thenReturn(response(OpportunityStatus.DRAFT));

        mockMvc.perform(post("/api/recruiters/10/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/recruiters/10/opportunities/20"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldRejectInvalidOpportunity() throws Exception {
        mockMvc.perform(post("/api/recruiters/10/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","description":"","vacancyCount":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.modality").exists());
    }

    @Test
    void shouldListOwnedOpportunities() throws Exception {
        when(service.listOwned(10L)).thenReturn(List.of(response(OpportunityStatus.DRAFT)));

        mockMvc.perform(get("/api/recruiters/10/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20));
    }

    @Test
    void shouldPublishOpportunity() throws Exception {
        when(service.publish(10L, 20L)).thenReturn(response(OpportunityStatus.PUBLISHED));

        mockMvc.perform(post("/api/recruiters/10/opportunities/20/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void shouldReturnUnprocessableEntityForInvalidTransition() throws Exception {
        when(service.close(10L, 20L)).thenThrow(new BusinessException("Invalid transition"));

        mockMvc.perform(post("/api/recruiters/10/opportunities/20/close"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Invalid transition"));
    }

    @Test
    void shouldReturnConflictForConcurrentModification() throws Exception {
        when(service.publish(10L, 20L)).thenThrow(
                new ObjectOptimisticLockingFailureException("Opportunity", 20L));

        mockMvc.perform(post("/api/recruiters/10/opportunities/20/publish"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Resource was modified by another request. Reload it and try again"));
    }

    private String validRequest() {
        return """
                {
                  "title":"Backend internship",
                  "description":"Develop APIs",
                  "requirements":"Java",
                  "modality":"INTERNSHIP",
                  "location":"Remote",
                  "vacancyCount":2,
                  "registrationStartsAt":"2099-01-01T00:00:00Z",
                  "registrationEndsAt":"2099-02-01T00:00:00Z"
                }
                """;
    }

    private OpportunityResponse response(OpportunityStatus status) {
        return new OpportunityResponse(20L, 10L, "Backend internship", "Develop APIs", "Java",
                OpportunityModality.INTERNSHIP, "Remote", 2,
                Instant.parse("2099-01-01T00:00:00Z"), Instant.parse("2099-02-01T00:00:00Z"),
                status, null, null);
    }
}
