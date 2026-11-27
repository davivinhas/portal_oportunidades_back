package com.example.portal_oportunidades_back.auth.dto;

import java.util.List;

public record CurrentUserResponse(Long id, String email, List<String> roles) { }
