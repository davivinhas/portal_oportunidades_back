package com.example.portal_oportunidades_back.auth.controller;
import com.example.portal_oportunidades_back.auth.dto.*;
import com.example.portal_oportunidades_back.auth.service.*;
import com.example.portal_oportunidades_back.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authentication;
    private final RegistrationService registration;
    private final boolean secure;
    private final String sameSite;
    public AuthenticationController(AuthenticationService authentication, RegistrationService registration,
            @Value("${app.auth.cookie.secure:true}") boolean secure,
            @Value("${app.auth.cookie.same-site:Lax}") String sameSite) {
        this.authentication = authentication; this.registration = registration;
        this.secure = secure; this.sameSite = sameSite;
    }
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) { return new CsrfResponse(token.getHeaderName(), token.getToken()); }
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get the current authenticated user",
            description = "Validates the access_token cookie and returns the current account and roles; returns 401 if unauthenticated.")
    public ResponseEntity<CurrentUserResponse> me(Authentication principal) {
        if (principal == null || !(principal.getPrincipal() instanceof AuthenticatedUser user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var roles = principal.getAuthorities().stream().map(authority -> authority.getAuthority())
                .filter(role -> role.startsWith("ROLE_"))
                .map(role -> role.substring(5)).sorted().toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CurrentUserResponse(user.id(), user.email(), roles));
    }
    @PostMapping("/register/student")
    public ResponseEntity<RegistrationResponse> registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registration.registerStudent(request));
    }
    @PostMapping("/register/recruiter")
    public ResponseEntity<RegistrationResponse> registerRecruiter(@Valid @RequestBody RecruiterRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registration.registerRecruiter(request));
    }
    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        var token = authentication.login(request);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie(token.accessToken(), token.expiresIn()).toString()).build();
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", 0).toString()).build();
    }
    private ResponseCookie cookie(String value, long maxAge) {
        return ResponseCookie.from("access_token", value).httpOnly(true).secure(secure).sameSite(sameSite)
                .path("/").maxAge(maxAge).build();
    }
    public record CsrfResponse(String headerName, String token) { }
}
