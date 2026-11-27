package com.example.portal_oportunidades_back.auth.service;
import com.example.portal_oportunidades_back.auth.dto.*;
import com.example.portal_oportunidades_back.auth.repository.*;
import java.time.Instant;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    public AuthenticationService(UserRepository users, PasswordEncoder passwords, JwtEncoder encoder) {
        this.users = users; this.passwords = passwords; this.encoder = encoder;
    }
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        var user = users.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (user == null || !user.isActive() || !passwords.matches(request.password(), user.getPasswordHash()))
            throw new BadCredentialsException("Invalid credentials");
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("linkedufma").subject(user.getId().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(3600)).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", 3600);
    }
}
