package com.example.portal_oportunidades_back.profile.entity;

import com.example.portal_oportunidades_back.auth.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class RecruiterTest {

    @Test
    void shouldUpdateOwnProfileData() {
        Recruiter recruiter = new Recruiter(mock(User.class), RecruiterType.COMPANY, "Company", true);

        recruiter.updateProfile(RecruiterType.PROFESSOR, "Professor Name");

        assertEquals(RecruiterType.PROFESSOR, recruiter.getType());
        assertEquals("Professor Name", recruiter.getOrganizationName());
    }

    @Test
    void shouldRejectBlankOrganizationName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Recruiter(mock(User.class), RecruiterType.COMPANY, " ", true));
    }
}
