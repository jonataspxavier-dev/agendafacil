package com.agendafacil.backend.controller;

import com.agendafacil.backend.dto.AuthResponse;
import com.agendafacil.backend.dto.LoginRequest;
import com.agendafacil.backend.dto.RefreshRequest;
import com.agendafacil.backend.dto.RegisterRequest;
import com.agendafacil.backend.dto.UsuarioResponse;
import com.agendafacil.backend.entity.Usuario;
import com.agendafacil.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegisterRequest request) {
        Usuario usuario = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(usuario));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = authService.getAuthenticatedUser(jwt);
        return ResponseEntity.ok(UsuarioResponse.from(usuario));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RefreshRequest request) {
        authService.logout(jwt, request);
        return ResponseEntity.noContent().build();
    }
}
