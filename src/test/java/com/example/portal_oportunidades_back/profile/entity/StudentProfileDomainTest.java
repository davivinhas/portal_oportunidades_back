package com.example.portal_oportunidades_back.profile.entity;

import com.example.portal_oportunidades_back.auth.entity.User;
import com.example.portal_oportunidades_back.exception.BusinessException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class StudentProfileDomainTest {
    private final Student student = new Student(mock(User.class), "123", "Computação");
    private final LocalDate today = LocalDate.of(2026, 10, 9);

    @Test
    void normalizesProfileAndAllowsOptionalFields() {
        student.updateProfile(" 123 ", " Computação ", (short) 2, " ", " Resumo ", null, "");
        assertThat(student.getCourse()).isEqualTo("Computação");
        assertThat(student.getSummary()).isEqualTo("Resumo");
        assertThat(student.getPhone()).isNull();
        assertThat(student.getInterests()).isNull();
    }

    @Test
    void rejectsInvalidAcademicData() {
        assertThatThrownBy(() -> student.updateProfile("", "Course", null, null, null, null, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> student.updateProfile("123", " ", null, null, null, null, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> student.updateProfile("123", "Course", (short) 0, null, null, null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validatesExperiencePeriodAndCurrentState() {
        assertThatThrownBy(() -> experience(today.plusDays(1), null, true)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> experience(today, today.minusDays(1), false)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> experience(today, null, false)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> experience(today, today, true)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> experience(today, today.plusDays(1), false)).isInstanceOf(BusinessException.class);
        assertThat(experience(today, today, false).isCurrent()).isFalse();
        assertThat(experience(today, null, true).isCurrent()).isTrue();
    }

    private ProfessionalExperience experience(LocalDate start, LocalDate end, boolean current) {
        return new ProfessionalExperience(student, "Developer", "Company", start, end, current, null, today);
    }
}
