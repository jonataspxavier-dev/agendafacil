package com.agendafacil.backend.service;

import com.agendafacil.backend.dto.AuthResponse;
import com.agendafacil.backend.dto.LoginRequest;
import com.agendafacil.backend.dto.RefreshRequest;
import com.agendafacil.backend.dto.RegisterRequest;
import com.agendafacil.backend.entity.Perfil;
import com.agendafacil.backend.entity.RefreshToken;
import com.agendafacil.backend.entity.Usuario;
import com.agendafacil.backend.exception.DuplicateEmailException;
import com.agendafacil.backend.repository.RefreshTokenRepository;
import com.agendafacil.backend.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long accessTokenTtlMinutes;
    private final long refreshTokenTtlDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UsuarioRepository usuarioRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes,
            @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    @Transactional
    public Usuario register(RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException(request.email());
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(Perfil.CLIENTE);
        usuario.setCriadoEm(Instant.now());

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new BadCredentialsException("E-mail ou senha inválidos");
        }

        return newTokenPair(usuario);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hashToken(request.refreshToken()))
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));

        if (stored.isRevogado() || stored.getExpiraEm().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token inválido");
        }

        stored.setRevogado(true);
        refreshTokenRepository.save(stored);

        return newTokenPair(stored.getUsuario());
    }

    @Transactional
    public void logout(Jwt jwt, RefreshRequest request) {
        Long userId = Long.valueOf(jwt.getSubject());
        refreshTokenRepository.findByTokenHash(hashToken(request.refreshToken()))
                .filter(token -> token.getUsuario().getId().equals(userId))
                .ifPresent(token -> {
                    token.setRevogado(true);
                    refreshTokenRepository.save(token);
                });
    }

    public Usuario getAuthenticatedUser(Jwt jwt) {
        Long id = Long.valueOf(jwt.getSubject());
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Usuário do token não encontrado: " + id));
    }

    private AuthResponse newTokenPair(Usuario usuario) {
        String accessToken = issueAccessToken(usuario);
        String refreshToken = issueAndPersistRefreshToken(usuario);
        return new AuthResponse(accessToken, refreshToken, accessTokenTtlMinutes * 60);
    }

    private String issueAccessToken(Usuario usuario) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("role", usuario.getPerfil().name())
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String issueAndPersistRefreshToken(Usuario usuario) {
        String rawToken = generateRawToken();

        RefreshToken entity = new RefreshToken();
        entity.setUsuario(usuario);
        entity.setTokenHash(hashToken(rawToken));
        entity.setExpiraEm(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS));
        entity.setRevogado(false);
        entity.setCriadoEm(Instant.now());
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
