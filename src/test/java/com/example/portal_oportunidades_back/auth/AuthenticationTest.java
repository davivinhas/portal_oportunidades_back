package com.example.portal_oportunidades_back.auth;

import com.example.portal_oportunidades_back.auth.dto.LoginRequest;
import com.example.portal_oportunidades_back.auth.entity.User;
import com.example.portal_oportunidades_back.auth.repository.*;
import com.example.portal_oportunidades_back.auth.service.AuthenticationService;
import com.example.portal_oportunidades_back.config.SecurityConfiguration;
import com.example.portal_oportunidades_back.profile.repository.*;
import com.example.portal_oportunidades_back.security.JwtAuthenticationFilter;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationTest {
    private final UserRepository users = mock(UserRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final RecruiterRepository recruiters = mock(RecruiterRepository.class);
    private final AdministratorRepository admins = mock(AdministratorRepository.class);
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final SecretKeySpec key = new SecretKeySpec("test-secret-that-is-at-least-32-bytes".getBytes(), "HmacSHA256");
    private final JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    private final NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
    private final AuthenticationService service = new AuthenticationService(users, passwords, encoder);

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void loginTokenAuthenticatesAndUsesCurrentRoles() throws Exception {
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L); when(user.isActive()).thenReturn(true);
        when(user.getPasswordHash()).thenReturn(passwords.encode("password"));
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(users.findById(42L)).thenReturn(Optional.of(user)); when(students.existsById(42L)).thenReturn(true);
        var token = service.login(new LoginRequest("user@example.com", "password")).accessToken();
        var request = new MockHttpServletRequest(); request.setCookies(new jakarta.servlet.http.Cookie("access_token", token));
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        new JwtAuthenticationFilter(decoder, users, students, recruiters, admins).doFilter(request, response, chain);
        assertEquals("42", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")));
        assertEquals(200, response.getStatus());
        when(user.isActive()).thenReturn(false);
        SecurityContextHolder.clearContext();
        var denied = new MockHttpServletResponse();
        new JwtAuthenticationFilter(decoder, users, students, recruiters, admins).doFilter(request, denied, new MockFilterChain());
        assertEquals(401, denied.getStatus());
    }
    @Test void wrongPasswordIsRejected() {
        User user = mock(User.class); when(user.isActive()).thenReturn(true);
        when(user.getPasswordHash()).thenReturn(passwords.encode("right"));
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        assertThrows(BadCredentialsException.class, () -> service.login(new LoginRequest("user@example.com", "wrong")));
    }
    @Test void malformedTokenIsRejected() throws Exception {
        var request = new MockHttpServletRequest(); request.setCookies(new jakarta.servlet.http.Cookie("access_token", "invalid"));
        var response = new MockHttpServletResponse();
        new JwtAuthenticationFilter(decoder, users, students, recruiters, admins).doFilter(request, response, new MockFilterChain());
        assertEquals(401, response.getStatus());
    }
}
