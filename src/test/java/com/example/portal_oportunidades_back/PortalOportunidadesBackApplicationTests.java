package com.example.portal_oportunidades_back;

import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import com.example.portal_oportunidades_back.profile.repository.StudentRepository;
import com.example.portal_oportunidades_back.profile.repository.RecruiterFollowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PortalOportunidadesBackApplicationTests {

    @MockitoBean
    private OpportunityRepository opportunityRepository;

    @MockitoBean
    private RecruiterRepository recruiterRepository;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private RecruiterFollowRepository recruiterFollowRepository;

    @Test
    void contextLoads() {
    }

}
