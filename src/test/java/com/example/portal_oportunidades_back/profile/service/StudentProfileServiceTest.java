package com.example.portal_oportunidades_back.profile.service;

import com.example.portal_oportunidades_back.auth.entity.User;
import com.example.portal_oportunidades_back.auth.repository.UserRepository;
import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.profile.dto.*;
import com.example.portal_oportunidades_back.profile.entity.*;
import com.example.portal_oportunidades_back.profile.mapper.StudentProfileMapper;
import com.example.portal_oportunidades_back.profile.repository.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class StudentProfileServiceTest {
    private final StudentRepository students = mock(StudentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ProfessionalExperienceRepository experiences = mock(ProfessionalExperienceRepository.class);
    private final StudentProfileMapper mapper = mock(StudentProfileMapper.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final StudentProfileService service = new StudentProfileService(students, users, experiences, mapper,
            Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC), entityManager);

    @Test
    void reportsMissingProfileAndUser() {
        assertThatThrownBy(() -> service.getProfile(1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.updateProfile(1L, request(List.of()))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsDuplicateRegistration() {
        when(students.existsByRegistrationNumberAndIdNot("123", 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.updateProfile(1L, request(List.of()))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(experiences);
    }

    @Test
    void createsProfileForExistingUser() {
        when(users.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(students.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.updateProfile(1L, request(List.of()));
        verify(students).saveAndFlush(any(Student.class));
        verifyNoInteractions(entityManager);
    }

    @Test
    void updatesOwnedExperienceAndDeletesOmittedOne() {
        Student student = new Student(mock(User.class), "123", "Course");
        var first = experience(student, 10L);
        var removed = experience(student, 20L);
        when(students.findById(1L)).thenReturn(Optional.of(student));
        when(experiences.findAllByStudentIdOrderByStartDateDescIdAsc(1L)).thenReturn(List.of(first, removed));
        service.updateProfile(1L, request(List.of(item(10L))));
        assertThat(first.getPosition()).isEqualTo("Updated");
        verify(experiences).deleteAll(List.of(removed));
        verify(entityManager).lock(student, jakarta.persistence.LockModeType.OPTIMISTIC_FORCE_INCREMENT);
    }

    @Test
    void rejectsForeignAndDuplicateIdsBeforeChangingProfile() {
        Student student = new Student(mock(User.class), "123", "Original");
        when(students.findById(1L)).thenReturn(Optional.of(student));
        when(experiences.findAllByStudentIdOrderByStartDateDescIdAsc(1L))
                .thenReturn(List.of(experience(student, 10L)));
        assertThatThrownBy(() -> service.updateProfile(1L, request(List.of(item(99L)))))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.updateProfile(1L, request(List.of(item(10L), item(10L)))))
                .isInstanceOf(BusinessException.class);
        assertThat(student.getCourse()).isEqualTo("Original");
        verify(experiences, never()).save(any());
        verify(experiences, never()).deleteAll(any());
    }

    private StudentProfileRequest request(List<ExperienceRequest> items) {
        return new StudentProfileRequest("123", "Course", null, null, null, null, null, items);
    }
    private ExperienceRequest item(Long id) {
        return new ExperienceRequest(id, "Updated", "Company", LocalDate.of(2025, 1, 1), null, true, null);
    }
    private ProfessionalExperience experience(Student student, Long id) {
        var value = new ProfessionalExperience(student, "Original", "Company", LocalDate.of(2025, 1, 1),
                null, true, null, LocalDate.of(2026, 10, 9));
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
