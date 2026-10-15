package com.example.portal_oportunidades_back.application.service;

import com.example.portal_oportunidades_back.application.dto.*;
import com.example.portal_oportunidades_back.application.mapper.ApplicationMapper;
import com.example.portal_oportunidades_back.application.repository.ApplicationRepository;
import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.profile.repository.StudentRepository;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationService {
    private final ApplicationRepository applications;
    private final StudentRepository students;
    private final OpportunityRepository opportunities;
    private final ApplicationMapper mapper;
    private final Clock clock;

    public ApplicationService(ApplicationRepository applications, StudentRepository students,
            OpportunityRepository opportunities, ApplicationMapper mapper, Clock clock) {
        this.applications = applications;
        this.students = students;
        this.opportunities = opportunities;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public ApplicationResponse create(Long opportunityId, Long studentId) {
        var student = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        // Serialize eligibility checking with opportunity updates and concurrent applications.
        Opportunity opportunity = opportunities.findByIdForApplication(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + opportunityId));
        if (opportunity.getDeletedAt() != null)
            throw new ResourceNotFoundException("Opportunity not found: " + opportunityId);
        if (applications.existsByStudentIdAndOpportunityId(studentId, opportunityId))
            throw new ConflictException("Student has already applied to this opportunity");
        Application application = new Application(student, opportunity, clock.instant());
        try {
            return mapper.toResponse(applications.saveAndFlush(application));
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                        && "uk_application_student_opportunity".equals(violation.getConstraintName())) {
                    throw new ConflictException("Student has already applied to this opportunity");
                }
            }
            throw exception;
        }
    }

    public ApplicationPageResponse listOwned(Long studentId, ApplicationStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new BadRequestException("Page must be non-negative and size must be between 1 and 100");
        ensureStudentExists(studentId);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("appliedAt"), Sort.Order.desc("id")));
        Page<Application> result = status == null ? applications.findAllByStudentId(studentId, pageable)
                : applications.findAllByStudentIdAndStatus(studentId, status, pageable);
        return new ApplicationPageResponse(result.getContent().stream().map(mapper::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), result.hasNext());
    }

    public ApplicationResponse getOwned(Long studentId, Long applicationId) {
        ensureStudentExists(studentId);
        return mapper.toResponse(findOwned(studentId, applicationId));
    }

    @Transactional
    public void cancel(Long studentId, Long applicationId) {
        ensureStudentExists(studentId);
        Application application = findOwned(studentId, applicationId);
        application.cancel();
    }

    private Application findOwned(Long studentId, Long applicationId) {
        return applications.findByIdAndStudentId(applicationId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private void ensureStudentExists(Long studentId) {
        if (!students.existsById(studentId))
            throw new ResourceNotFoundException("Student not found: " + studentId);
    }
}
