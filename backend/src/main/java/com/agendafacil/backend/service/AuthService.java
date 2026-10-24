package com.agendafacil.backend.service;

import com.agendafacil.backend.dto.RegisterRequest;
import com.agendafacil.backend.entity.Perfil;
import com.agendafacil.backend.entity.Usuario;
import com.agendafacil.backend.exception.DuplicateEmailException;
import com.agendafacil.backend.repository.UsuarioRepository;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
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
}
