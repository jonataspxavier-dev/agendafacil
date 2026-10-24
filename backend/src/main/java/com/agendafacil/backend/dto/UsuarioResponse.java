package com.agendafacil.backend.dto;

import com.agendafacil.backend.entity.Perfil;
import com.agendafacil.backend.entity.Usuario;

public record UsuarioResponse(Long id, String nome, String email, Perfil perfil) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil());
    }
}
