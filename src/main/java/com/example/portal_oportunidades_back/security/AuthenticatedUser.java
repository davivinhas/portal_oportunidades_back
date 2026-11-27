package com.example.portal_oportunidades_back.security;

import java.security.Principal;

public record AuthenticatedUser(Long id, String email) implements Principal {
    @Override
    public String getName() { return id.toString(); }
}
