package com.example.portal_oportunidades_back.auth.service;

import com.example.portal_oportunidades_back.auth.entity.Administrator;
import com.example.portal_oportunidades_back.auth.entity.User;
import com.example.portal_oportunidades_back.auth.repository.AdministratorRepository;
import com.example.portal_oportunidades_back.auth.repository.UserRepository;
import com.example.portal_oportunidades_back.exception.ConflictException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministratorBootstrapService {
    private final UserRepository users;
    private final AdministratorRepository administrators;
    private final PasswordEncoder passwords;

    public AdministratorBootstrapService(UserRepository users, AdministratorRepository administrators,
            PasswordEncoder passwords) {
        this.users = users; this.administrators = administrators; this.passwords = passwords;
    }

    @Transactional
    public Long create(String name, String email, char[] password) {
        String normalizedEmail = email == null ? "" : email.trim();
        if (users.findByEmailIgnoreCase(normalizedEmail).isPresent())
            throw new ConflictException("Email is already in use: " + normalizedEmail);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) throw new IllegalArgumentException("A valid email is required");
        if (password == null || password.length < 8 || new String(password).getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Password must contain at least 8 characters and at most 72 UTF-8 bytes");
        User user = users.saveAndFlush(new User(name, normalizedEmail, passwords.encode(new String(password))));
        administrators.save(new Administrator(user));
        return user.getId();
    }
}
