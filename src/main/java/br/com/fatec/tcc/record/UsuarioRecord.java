package br.com.fatec.tcc.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UsuarioRecord(Long id,
		@NotBlank(message = "Nome é obrigatório") String nomeCompleto,
		@NotBlank(message = "Email é obrigatório") String email,
		@NotBlank(message = "A senha é obrigatória") String senha,
		@NotBlank(message = "A confirmação da senha é obrigatória") String confirmarSenha,
		@NotNull(message = "Carga máxima é obrigatória") @Positive(message = "Carga máxima deve ser positiva") Double cargaMaxima,
		@NotNull(message = "Marca é obrigatória") Long marcaId) {

}
