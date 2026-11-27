package com.example.portal_oportunidades_back.auth.dto;
public record TokenResponse(String accessToken, String tokenType, long expiresIn) { }
