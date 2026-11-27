package com.example.portal_oportunidades_back.administration.service;

import com.example.portal_oportunidades_back.administration.dto.AdminUserResponse;
import com.example.portal_oportunidades_back.auth.repository.*;
import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class UserAdministrationService {
    private final UserRepository users;
    private final com.example.portal_oportunidades_back.profile.repository.StudentRepository students;
    private final com.example.portal_oportunidades_back.profile.repository.RecruiterRepository recruiters;
    private final AdministratorRepository administrators;

    public UserAdministrationService(UserRepository users, com.example.portal_oportunidades_back.profile.repository.StudentRepository students,
            com.example.portal_oportunidades_back.profile.repository.RecruiterRepository recruiters,
            AdministratorRepository administrators) {
        this.users = users; this.students = students; this.recruiters = recruiters; this.administrators = administrators;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new com.example.portal_oportunidades_back.exception.BadRequestException("Invalid pagination");
        return users.findAll(PageRequest.of(page, size, Sort.by("id").ascending())).map(user -> {
            var roles = new ArrayList<String>();
            if (students.existsById(user.getId())) roles.add("STUDENT");
            if (recruiters.existsById(user.getId())) roles.add("RECRUITER");
            if (administrators.existsById(user.getId())) roles.add("ADMIN");
            return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.isActive(), List.copyOf(roles));
        });
    }

    @Transactional
    public void suspend(Long id) { find(id).suspend(); }

    @Transactional
    public void reactivate(Long id) { find(id).reactivate(); }

    private com.example.portal_oportunidades_back.auth.entity.User find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
