package com.agendafacil.backend.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresInSeconds) {
}
