package br.com.fatec.tcc.dto;

import br.com.fatec.tcc.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioDTO(
    @NotBlank(message = "Nome completo é obrigatório")
    String nomeCompleto,

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Pattern(regexp = ".+@fatec\\.sp\\.gov\\.br$", message = "Use e-mail @fatec.sp.gov.br")
    String email,

    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    String senha,

    String confirmarSenha,   // validação manual no controller/service

    @NotBlank(message = "Matrícula é obrigatória")
    @Pattern(regexp = "[A-Za-z0-9]{8,15}", message = "Matrícula deve ter entre 8 e 15 caracteres alfanuméricos")
    String matricula,

    @NotBlank(message = "Curso é obrigatório")
    String curso,

    Usuario.Periodo periodo,

    String fotoPerfil
) {}