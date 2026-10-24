package com.agendafacil.backend.dto;

public record AuthResponse(String accessToken, long expiresInSeconds) {
}
