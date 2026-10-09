package com.example.portal_oportunidades_back.profile.controller;

import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.profile.dto.FollowingResponse;
import com.example.portal_oportunidades_back.profile.service.RecruiterFollowService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RecruiterFollowControllerTest {
    private final RecruiterFollowService service = mock(RecruiterFollowService.class);
    private final MockMvc mvc = standaloneSetup(new RecruiterFollowController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    private static final String PATH = "/api/students/1/following/2";

    @Test
    void followsWithNoContent() throws Exception {
        mvc.perform(put(PATH)).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).follow(1L, 2L);
    }

    @Test
    void unfollowsWithNoContent() throws Exception {
        mvc.perform(delete(PATH)).andExpect(status().isNoContent());
        verify(service).unfollow(1L, 2L);
    }

    @Test
    void returnsFollowingState() throws Exception {
        when(service.getFollowing(1L, 2L)).thenReturn(new FollowingResponse(true));
        mvc.perform(get(PATH)).andExpect(status().isOk()).andExpect(jsonPath("$.following").value(true));
    }

    @Test
    void returnsStandardNotFoundError() throws Exception {
        doThrow(new ResourceNotFoundException("Recruiter not found: 2")).when(service).follow(1L, 2L);
        mvc.perform(put(PATH)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recruiter not found: 2"));
    }

    @Test
    void rejectsNonNumericIdentifiers() throws Exception {
        mvc.perform(put("/api/students/invalid/following/2")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
