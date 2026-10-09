package com.example.portal_oportunidades_back.application.service;

import com.example.portal_oportunidades_back.application.dto.*;
import com.example.portal_oportunidades_back.application.mapper.ApplicationMapper;
import com.example.portal_oportunidades_back.application.repository.ApplicationRepository;
import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.profile.entity.Student;
import com.example.portal_oportunidades_back.profile.repository.StudentRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.dao.DataIntegrityViolationException;
import static com.example.portal_oportunidades_back.opportunity.OpportunityFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ApplicationServiceTest {
    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final OpportunityRepository opportunities = mock(OpportunityRepository.class);
    private final ApplicationMapper mapper = mock(ApplicationMapper.class);
    private final ApplicationService service = new ApplicationService(applications, students, opportunities, mapper,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final Student student = mock(Student.class);
    private Opportunity opportunity;

    @BeforeEach
    void prepare() {
        opportunity = draft(1L);
        opportunity.publish(NOW);
        when(students.findById(2L)).thenReturn(Optional.of(student));
        when(students.existsById(2L)).thenReturn(true);
        when(opportunities.findByIdForApplication(1L)).thenReturn(Optional.of(opportunity));
    }

    @Test
    void createsSubmittedApplicationWithControlledTime() {
        when(applications.saveAndFlush(any())).thenAnswer(invocation -> {
            Application value = invocation.getArgument(0);
            assertThat(value.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
            assertThat(value.getAppliedAt()).isEqualTo(NOW);
            assertThat(value.getStudent()).isSameAs(student);
            return value;
        });
        service.create(1L, 2L);
        verify(opportunities).findByIdForApplication(1L);
        verify(applications).saveAndFlush(any());
    }

    @Test
    void rejectsMissingProfilesAndDeletedOpportunities() {
        assertThatThrownBy(() -> service.create(1L, 99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.create(99L, 2L)).isInstanceOf(ResourceNotFoundException.class);
        opportunity.softDelete(NOW);
        assertThatThrownBy(() -> service.create(1L, 2L)).isInstanceOf(ResourceNotFoundException.class);
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateAndClosedOpportunity() {
        when(applications.existsByStudentIdAndOpportunityId(2L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, 2L)).isInstanceOf(ConflictException.class);
        when(applications.existsByStudentIdAndOpportunityId(2L, 1L)).thenReturn(false);
        opportunity.close(NOW);
        assertThatThrownBy(() -> service.create(1L, 2L)).isInstanceOf(BusinessException.class);
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void translatesOnlyDuplicateConstraintViolations() {
        var violation = new org.hibernate.exception.ConstraintViolationException("duplicate",
                new java.sql.SQLException("duplicate", "23505"), "uk_application_student_opportunity");
        when(applications.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate", violation));
        assertThatThrownBy(() -> service.create(1L, 2L)).isInstanceOf(ConflictException.class);
        doThrow(new DataIntegrityViolationException("unrelated constraint")).when(applications).saveAndFlush(any());
        assertThatThrownBy(() -> service.create(1L, 2L)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void filtersAndPaginatesByOwnerWithStableOrder() {
        var application = new Application(student, opportunity, NOW);
        when(applications.findAllByStudentIdAndStatus(eq(2L), eq(ApplicationStatus.SUBMITTED), any()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(application), invocation.getArgument(2), 3));
        var result = service.listOwned(2L, ApplicationStatus.SUBMITTED, 0, 1);
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        verify(applications).findAllByStudentIdAndStatus(eq(2L), eq(ApplicationStatus.SUBMITTED),
                eq(PageRequest.of(0, 1, Sort.by(Sort.Order.desc("appliedAt"), Sort.Order.desc("id")))));
        when(applications.findAllByStudentId(eq(2L), any())).thenReturn(Page.empty(PageRequest.of(0, 20)));
        assertThat(service.listOwned(2L, null, 0, 20).items()).isEmpty();
    }

    @Test
    void validatesPaginationAndMissingOwners() {
        assertThatThrownBy(() -> service.listOwned(2L, null, -1, 20)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.listOwned(2L, null, 0, 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.listOwned(2L, null, 0, 101)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.listOwned(99L, null, 0, 20)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void protectsDetailAndCancellationByOwner() {
        assertThatThrownBy(() -> service.getOwned(2L, 99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.cancel(2L, 99L)).isInstanceOf(ResourceNotFoundException.class);
        Application owned = new Application(student, opportunity, NOW);
        when(applications.findByIdAndStudentId(10L, 2L)).thenReturn(Optional.of(owned));
        service.getOwned(2L, 10L);
        verify(mapper).toResponse(owned);
        service.cancel(2L, 10L);
        assertThat(owned.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        verify(applications, never()).save(any());
    }
}
