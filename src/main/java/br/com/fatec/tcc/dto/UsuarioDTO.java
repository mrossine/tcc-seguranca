package br.com.fatec.tcc.dto;

import br.com.fatec.tcc.model.Usuario;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
	
	private String nomeCompleto;
	private String email;
	private String senha;
	private String confirmarSenha;
	private String matricula;
	private String curso;
	private Usuario.Periodo periodo;
	private String fotoPerfil;
	
}