package com.example.portal_oportunidades_back.application.controller;

import com.example.portal_oportunidades_back.application.dto.*;
import com.example.portal_oportunidades_back.application.service.ApplicationService;
import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.opportunity.entity.ApplicationStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApplicationControllerTest {
    private final ApplicationService service = mock(ApplicationService.class);
    private final MockMvc mvc = standaloneSetup(new ApplicationController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    private final ApplicationResponse response = new ApplicationResponse(10L, 2L, null,
            ApplicationStatus.SUBMITTED, Instant.parse("2026-10-09T12:00:00Z"), null, null);

    @Test
    void createsWithLocationAndInitialState() throws Exception {
        when(service.create(1L, 2L)).thenReturn(response);
        mvc.perform(post("/api/opportunities/1/applications").contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":2}")).andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/students/2/applications/10"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void rejectsInvalidCreationRequest() throws Exception {
        for (String request : List.of("{}", "{\"studentId\":-1}", "{\"studentId\":0}"))
            mvc.perform(post("/api/opportunities/1/applications").contentType(MediaType.APPLICATION_JSON)
                    .content(request)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void listsWithPaginationAndOptionalFilter() throws Exception {
        when(service.listOwned(2L, ApplicationStatus.SUBMITTED, 1, 5))
                .thenReturn(new ApplicationPageResponse(List.of(response), 1, 5, 6, 2, false));
        mvc.perform(get("/api/students/2/applications").param("status", "SUBMITTED")
                .param("page", "1").param("size", "5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.items[0].id").value(10));
        mvc.perform(get("/api/students/2/applications"));
        verify(service).listOwned(2L, null, 0, 20);
        mvc.perform(get("/api/students/2/applications").param("status", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsDetailAndCancelsWithoutDeleting() throws Exception {
        when(service.getOwned(2L, 10L)).thenReturn(response);
        mvc.perform(get("/api/students/2/applications/10")).andExpect(status().isOk());
        mvc.perform(post("/api/students/2/applications/10/cancel")).andExpect(status().isNoContent());
        verify(service).cancel(2L, 10L);
    }

    @Test
    void returnsStandardConflictBusinessAndNotFoundErrors() throws Exception {
        when(service.create(1L, 2L)).thenThrow(new ConflictException("Already applied"))
                .thenThrow(new BusinessException("Closed opportunity"));
        mvc.perform(post("/api/opportunities/1/applications").contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":2}")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Already applied"));
        mvc.perform(post("/api/opportunities/1/applications").contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":2}")).andExpect(status().isUnprocessableEntity());
        when(service.getOwned(2L, 99L)).thenThrow(new ResourceNotFoundException("Application not found"));
        mvc.perform(get("/api/students/2/applications/99")).andExpect(status().isNotFound());
    }
}
