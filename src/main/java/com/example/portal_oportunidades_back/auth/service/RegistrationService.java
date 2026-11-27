package com.example.portal_oportunidades_back.auth.service;
import com.example.portal_oportunidades_back.auth.dto.*;
import com.example.portal_oportunidades_back.auth.entity.User;
import com.example.portal_oportunidades_back.auth.repository.UserRepository;
import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.administration.repository.CatalogItemRepository;
import com.example.portal_oportunidades_back.administration.service.CatalogAdministrationService;
import com.example.portal_oportunidades_back.profile.entity.*;
import com.example.portal_oportunidades_back.profile.repository.*;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final UserRepository users;
    private final StudentRepository students;
    private final RecruiterRepository recruiters;
    private final PasswordEncoder passwords;
    private final CatalogItemRepository catalogs;
    @org.springframework.beans.factory.annotation.Autowired
    public RegistrationService(UserRepository users, StudentRepository students, RecruiterRepository recruiters,
            PasswordEncoder passwords, CatalogItemRepository catalogs) {
        this.users = users; this.students = students; this.recruiters = recruiters; this.passwords = passwords; this.catalogs = catalogs;
    }
    public RegistrationService(UserRepository users, StudentRepository students, RecruiterRepository recruiters, PasswordEncoder passwords) {
        this(users, students, recruiters, passwords, null);
    }
    @Transactional
    public RegistrationResponse registerStudent(StudentRegistrationRequest request) {
        User user = createUser(request.name(), request.email(), request.password());
        students.save(new Student(user, request.registrationNumber(), request.course()));
        return new RegistrationResponse(user.getId(), "STUDENT");
    }
    @Transactional
    public RegistrationResponse registerRecruiter(RecruiterRegistrationRequest request) {
        if (catalogs != null && !catalogs.existsByCategoryAndCodeAndActiveTrue(CatalogAdministrationService.RECRUITER_TYPE, request.type()))
            throw new BadRequestException("Recruiter type is not active in the catalog");
        User user = createUser(request.name(), request.email(), request.password());
        recruiters.save(new Recruiter(user, request.type(), request.organizationName(), false));
        return new RegistrationResponse(user.getId(), "RECRUITER");
    }
    private User createUser(String name, String email, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) throw new BadRequestException("Password must not exceed 72 UTF-8 bytes");
        if (users.findByEmailIgnoreCase(email.trim()).isPresent()) throw new ConflictException("Email is already in use");
        return users.save(new User(name, email, passwords.encode(password)));
    }
}
