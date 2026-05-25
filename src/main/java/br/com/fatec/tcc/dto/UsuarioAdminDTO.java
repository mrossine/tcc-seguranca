package br.com.fatec.tcc.dto;

public record UsuarioAdminDTO(
        Long id,
        String nomeCompleto,
        String email,
        String matricula,
        String curso,
        String fotoPerfil
) {}
