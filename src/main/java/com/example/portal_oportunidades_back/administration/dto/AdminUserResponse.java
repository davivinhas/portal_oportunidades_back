package com.example.portal_oportunidades_back.administration.dto;

import java.util.List;

public record AdminUserResponse(Long id, String name, String email, boolean active, List<String> roles) { }
