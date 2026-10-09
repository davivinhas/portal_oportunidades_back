package com.example.portal_oportunidades_back.profile.service;

import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.profile.dto.*;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentResumeServiceTest {
    private final StudentProfileService profiles = mock(StudentProfileService.class);
    private final StudentResumeService service = new StudentResumeService(profiles);

    @Test
    void producesReadablePdfWithAccentsAndNoExperience() throws Exception {
        when(profiles.getProfile(1L)).thenReturn(profile("José", null, List.of()));
        byte[] pdf = service.generate(1L);
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try (var document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document)).contains("José", "Formação acadêmica", "Computação");
        }
    }

    @Test
    void paginatesLongTextAndIncludesExperiencesAndUnsupportedGlyphs() throws Exception {
        var experience = new ExperienceResponse(1L, "Estagiário", "Universidade",
                LocalDate.of(2025, 1, 1), null, true, "Pesquisa");
        when(profiles.getProfile(1L)).thenReturn(profile("José", ("Descrição\n" + "x".repeat(200) + " ").repeat(100)
                + "\uD83E\uDDEE", List.of(experience)));
        try (var document = Loader.loadPDF(service.generate(1L))) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(new PDFTextStripper().getText(document)).contains("Estagiário", "Universidade", "Atual", "Pesquisa");
        }
    }

    @Test
    void rejectsIncompleteProfile() {
        when(profiles.getProfile(1L)).thenReturn(profile(" ", null, List.of()));
        assertThatThrownBy(() -> service.generate(1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Incomplete profile");
    }

    private StudentProfileResponse profile(String name, String summary, List<ExperienceResponse> items) {
        return new StudentProfileResponse(1L, name, "student@example.test", "123", "Computação", (short) 2,
                null, summary, null, null, items);
    }
}
