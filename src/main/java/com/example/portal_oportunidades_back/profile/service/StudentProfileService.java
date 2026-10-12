package com.example.portal_oportunidades_back.profile.service;

import com.example.portal_oportunidades_back.auth.repository.UserRepository;
import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.profile.dto.*;
import com.example.portal_oportunidades_back.profile.entity.*;
import com.example.portal_oportunidades_back.profile.mapper.StudentProfileMapper;
import com.example.portal_oportunidades_back.profile.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentProfileService {
    private final StudentRepository students;
    private final UserRepository users;
    private final ProfessionalExperienceRepository experiences;
    private final StudentProfileMapper mapper;
    private final Clock clock;
    private final EntityManager entityManager;

    public StudentProfileService(StudentRepository students, UserRepository users,
            ProfessionalExperienceRepository experiences, StudentProfileMapper mapper,
            Clock clock, EntityManager entityManager) {
        this.students = students;
        this.users = users;
        this.experiences = experiences;
        this.mapper = mapper;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public StudentProfileResponse getProfile(Long studentId) {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found: " + studentId));
        return mapper.toResponse(student, experiences.findAllByStudentIdOrderByStartDateDescIdAsc(studentId));
    }

    @Transactional
    public StudentProfileResponse updateProfile(Long studentId, StudentProfileRequest request) {
        if (students.existsByRegistrationNumberAndIdNot(request.registrationNumber().trim(), studentId))
            throw new BusinessException("Registration number is already in use");
        Student student = students.findById(studentId).orElse(null);
        boolean creating = student == null;
        if (creating) {
            var user = users.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + studentId));
            student = new Student(user, request.registrationNumber(), request.course());
        } else {
            // Include experience-only changes in the aggregate's concurrency control.
            entityManager.lock(student, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        }
        List<ProfessionalExperience> existing = creating ? List.of()
                : experiences.findAllByStudentIdOrderByStartDateDescIdAsc(studentId);
        Map<Long, ProfessionalExperience> owned = new HashMap<>();
        existing.forEach(experience -> owned.put(experience.getId(), experience));
        Set<Long> retained = new HashSet<>();
        for (ExperienceRequest item : request.experiences()) {
            if (item.id() != null && !retained.add(item.id()))
                throw new BusinessException("Duplicate experience ID");
            if (item.id() != null && !owned.containsKey(item.id()))
                throw new ForbiddenOperationException("Experience does not belong to this student");
        }

        student.updateProfile(request.registrationNumber(), request.course(), request.semester(),
                request.phone(), request.summary(), request.skills(), request.interests());
        if (creating) student = students.saveAndFlush(student);
        LocalDate today = LocalDate.now(clock);
        for (ExperienceRequest item : request.experiences()) {
            if (item.id() == null) {
                experiences.save(new ProfessionalExperience(student, item.position(), item.organization(),
                        item.startDate(), item.endDate(), item.current(), item.description(), today));
            } else {
                owned.get(item.id()).update(item.position(), item.organization(), item.startDate(),
                        item.endDate(), item.current(), item.description(), today);
            }
        }
        experiences.deleteAll(existing.stream().filter(item -> !retained.contains(item.getId())).toList());
        experiences.flush();
        students.flush();
        return mapper.toResponse(student, experiences.findAllByStudentIdOrderByStartDateDescIdAsc(studentId));
    }
}
