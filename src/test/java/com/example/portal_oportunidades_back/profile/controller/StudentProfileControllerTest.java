package com.example.portal_oportunidades_back.profile.controller;

import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.profile.dto.*;
import com.example.portal_oportunidades_back.profile.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudentProfileControllerTest {
    private final StudentProfileService profiles = mock(StudentProfileService.class);
    private final StudentResumeService resumes = mock(StudentResumeService.class);
    private final MockMvc mvc = standaloneSetup(new StudentProfileController(profiles, resumes))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    private final StudentProfileResponse profile = new StudentProfileResponse(1L, "Student", "student@example.test",
            "123", "Course", null, null, null, null, null, List.of());

    @Test
    void returnsProfileAndSupportsCreationThroughPut() throws Exception {
        when(profiles.getProfile(1L)).thenReturn(profile);
        when(profiles.updateProfile(eq(1L), any())).thenReturn(profile);
        mvc.perform(get("/api/students/1/profile")).andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationNumber").value("123"));
        mvc.perform(put("/api/students/1/profile").contentType(MediaType.APPLICATION_JSON)
                .content("{\"registrationNumber\":\"123\",\"course\":\"Course\",\"experiences\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.experiences").isArray());
    }

    @Test
    void validatesRequiredFieldsAndNestedExperience() throws Exception {
        mvc.perform(put("/api/students/1/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.course").exists());
        mvc.perform(put("/api/students/1/profile").contentType(MediaType.APPLICATION_JSON)
                .content("{\"registrationNumber\":\"123\",\"course\":\"Course\",\"experiences\":[{}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(profiles);
    }

    @Test
    void downloadsPdfWithCorrectHeaders() throws Exception {
        when(resumes.generate(1L)).thenReturn(new byte[]{1, 2, 3});
        mvc.perform(get("/api/students/1/resume")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"curriculo-1.pdf\""))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void handlesMissingAndIncompleteProfilesAndForeignExperiences() throws Exception {
        when(profiles.getProfile(1L)).thenThrow(new ResourceNotFoundException("Profile not found"));
        when(resumes.generate(1L)).thenThrow(new BusinessException("Incomplete profile"));
        when(profiles.updateProfile(eq(1L), any())).thenThrow(new ForbiddenOperationException("Foreign experience"));
        mvc.perform(get("/api/students/1/profile")).andExpect(status().isNotFound());
        mvc.perform(get("/api/students/1/resume")).andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/students/1/profile").contentType(MediaType.APPLICATION_JSON)
                .content("{\"registrationNumber\":\"123\",\"course\":\"Course\",\"experiences\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mapsDatabaseAndVersionConflicts() throws Exception {
        when(profiles.updateProfile(eq(1L), any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate registration"))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException("Student", 1L));
        for (int attempt = 0; attempt < 2; attempt++)
            mvc.perform(put("/api/students/1/profile").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"registrationNumber\":\"123\",\"course\":\"Course\",\"experiences\":[]}"))
                    .andExpect(status().isConflict());
    }
}
