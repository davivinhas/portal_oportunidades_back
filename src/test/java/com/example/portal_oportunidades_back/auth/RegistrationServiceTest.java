package com.example.portal_oportunidades_back.auth;
import com.example.portal_oportunidades_back.auth.dto.*;
import com.example.portal_oportunidades_back.auth.entity.User;
import com.example.portal_oportunidades_back.auth.repository.UserRepository;
import com.example.portal_oportunidades_back.auth.service.RegistrationService;
import com.example.portal_oportunidades_back.exception.ConflictException;
import com.example.portal_oportunidades_back.profile.entity.*;
import com.example.portal_oportunidades_back.profile.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final RecruiterRepository recruiters = mock(RecruiterRepository.class);
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final RegistrationService service = new RegistrationService(users, students, recruiters, passwords);
    @Test void registersRecruiterWithHashedPasswordAndWithoutApproval() {
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        service.registerRecruiter(new RecruiterRegistrationRequest("Alice", "Alice@Example.com", "password1", RecruiterType.COMPANY, "Example"));
        var user = ArgumentCaptor.forClass(User.class); verify(users).save(user.capture());
        assertEquals("alice@example.com", user.getValue().getEmail());
        assertTrue(passwords.matches("password1", user.getValue().getPasswordHash()));
        var recruiter = ArgumentCaptor.forClass(Recruiter.class); verify(recruiters).save(recruiter.capture());
        assertFalse(recruiter.getValue().isAuthorized());
    }
    @Test void rejectsDuplicateEmailBeforeCreatingUser() {
        when(users.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(mock(User.class)));
        assertThrows(ConflictException.class, () -> service.registerStudent(new StudentRegistrationRequest("Alice", "alice@example.com", "password1", "123", "CS")));
        verify(users, never()).save(any()); verify(students, never()).save(any());
    }
}
