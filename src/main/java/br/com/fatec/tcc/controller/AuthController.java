package br.com.fatec.tcc.controller;

import br.com.fatec.tcc.dto.UsuarioDTO;
import br.com.fatec.tcc.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UsuarioService usuarioService;

	@PostMapping(value = "/register", produces = "application/json", consumes = "application/json")
	@ResponseBody
	public ResponseEntity<?> register(@RequestBody UsuarioDTO usuarioDTO) {
		Map<String, Object> response = new HashMap<>();

		try {
			log.info("=== RECEBENDO REQUISIÇÃO DE CADASTRO ===");
			log.info("Email: {}", usuarioDTO.getEmail());
			log.info("Nome: {}", usuarioDTO.getNomeCompleto());
			log.info("Matrícula: {}", usuarioDTO.getMatricula());

			// Validações
			if (usuarioDTO.getNomeCompleto() == null || usuarioDTO.getNomeCompleto().trim().isEmpty()) {
				response.put("message", "Nome completo é obrigatório");
				return ResponseEntity.badRequest().body(response);
			}

			if (usuarioDTO.getEmail() == null || usuarioDTO.getEmail().trim().isEmpty()) {
				response.put("message", "E-mail é obrigatório");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar e-mail institucional
			if (!usuarioDTO.getEmail().matches("^[a-zA-Z0-9._%+-]+@fatec\\.sp\\.gov\\.br$")) {
				response.put("message", "Use um e-mail @fatec.sp.gov.br");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar senha
			if (usuarioDTO.getSenha() == null || usuarioDTO.getSenha().length() < 6) {
				response.put("message", "A senha deve ter no mínimo 6 caracteres");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar confirmação de senha
			if (usuarioDTO.getConfirmarSenha() == null
					|| !usuarioDTO.getSenha().equals(usuarioDTO.getConfirmarSenha())) {
				response.put("message", "As senhas não coincidem");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar matrícula
			if (usuarioDTO.getMatricula() == null || !usuarioDTO.getMatricula().matches("^[A-Za-z0-9]{8,15}$")) {
				response.put("message", "Matrícula deve ter entre 8 e 15 caracteres alfanuméricos");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar curso
			if (usuarioDTO.getCurso() == null || usuarioDTO.getCurso().trim().isEmpty()) {
				response.put("message", "Curso é obrigatório");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar período
			if (usuarioDTO.getPeriodo() == null) {
				response.put("message", "Período é obrigatório");
				return ResponseEntity.badRequest().body(response);
			}

			// Tentar cadastrar
			usuarioService.cadastrar(usuarioDTO);

			response.put("success", true);
			response.put("message", "Usuário cadastrado com sucesso!");
			log.info("Usuário cadastrado com sucesso: {}", usuarioDTO.getEmail());
			return ResponseEntity.ok(response);

		} catch (RuntimeException e) {
			log.error("Erro ao cadastrar usuário: {}", e.getMessage());
			response.put("message", e.getMessage());
			return ResponseEntity.badRequest().body(response);
		} catch (Exception e) {
			log.error("Erro inesperado ao cadastrar usuário", e);
			response.put("message", "Erro interno no servidor: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}