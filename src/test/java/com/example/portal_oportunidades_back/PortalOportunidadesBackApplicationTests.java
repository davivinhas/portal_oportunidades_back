package com.example.portal_oportunidades_back;

import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
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
    private com.example.portal_oportunidades_back.profile.repository.StudentRepository studentRepository;

    @MockitoBean
    private com.example.portal_oportunidades_back.profile.repository.ProfessionalExperienceRepository professionalExperienceRepository;

    @MockitoBean
    private com.example.portal_oportunidades_back.auth.repository.UserRepository userRepository;

    @MockitoBean
    private jakarta.persistence.EntityManager entityManager;

    @MockitoBean
    private org.jooq.DSLContext dslContext;

    @MockitoBean
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @MockitoBean
    private com.example.portal_oportunidades_back.application.repository.ApplicationRepository applicationRepository;

    @MockitoBean
    private com.example.portal_oportunidades_back.profile.repository.RecruiterFollowRepository recruiterFollowRepository;

    @MockitoBean
    private com.example.portal_oportunidades_back.auth.repository.AdministratorRepository administratorRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.web.context.WebApplicationContext context;

    @Test
    void cookieLoginRequiresCsrf() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        var login = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"password\"}");
        mvc.perform(login).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        var csrf = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/csrf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        var body = csrf.getResponse().getContentAsString();
        var matcher = java.util.regex.Pattern.compile("\"token\":\"([^\"]+)\"").matcher(body);
        org.junit.jupiter.api.Assertions.assertTrue(matcher.find());
        var user = org.mockito.Mockito.mock(com.example.portal_oportunidades_back.auth.entity.User.class);
        org.mockito.Mockito.when(user.isActive()).thenReturn(true);
        org.mockito.Mockito.when(user.getId()).thenReturn(42L);
        org.mockito.Mockito.when(user.getPasswordHash()).thenReturn(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password"));
        org.mockito.Mockito.when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(java.util.Optional.of(user));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"password\"}")
                .cookie(csrf.getResponse().getCookie("XSRF-TOKEN")).header("X-XSRF-TOKEN", matcher.group(1)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));
    }

    @Test
    void onlyAdministratorCanAuthorizeRecruiter() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        var endpoint = "/api/admin/recruiters/42/authorization";
        var csrf = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(endpoint).with(csrf))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(endpoint).with(csrf)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("42").roles("RECRUITER")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        org.mockito.Mockito.verify(recruiterRepository, org.mockito.Mockito.never()).findById(org.mockito.ArgumentMatchers.any());
        var recruiter = new com.example.portal_oportunidades_back.profile.entity.Recruiter(
                org.mockito.Mockito.mock(com.example.portal_oportunidades_back.auth.entity.User.class),
                com.example.portal_oportunidades_back.profile.entity.RecruiterType.COMPANY, "Example", false);
        org.mockito.Mockito.when(recruiterRepository.findById(42L)).thenReturn(java.util.Optional.of(recruiter));
        org.mockito.Mockito.when(transactionManager.getTransaction(org.mockito.ArgumentMatchers.any()))
                .thenReturn(org.mockito.Mockito.mock(org.springframework.transaction.TransactionStatus.class));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(endpoint).with(csrf)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1").roles("ADMIN")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
        org.junit.jupiter.api.Assertions.assertTrue(recruiter.isAuthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(endpoint)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("1").roles("ADMIN")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void contextLoads() {
    }

}
