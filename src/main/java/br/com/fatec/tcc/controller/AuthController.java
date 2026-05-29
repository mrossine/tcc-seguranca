package br.com.fatec.tcc.controller;

import br.com.fatec.tcc.dto.UsuarioDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
	private final AuthenticationManager authenticationManager;

	@PostMapping(value = "/login", produces = "application/json", consumes = "application/json")
	@ResponseBody
	public ResponseEntity<?> loginMobile(@RequestBody Map<String, String> credentials,
										 HttpServletRequest request) {
		String email = credentials.get("email");
		String senha = credentials.get("senha");
		Map<String, Object> response = new HashMap<>();

		try {
			Authentication auth = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, senha));

			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(auth);
			SecurityContextHolder.setContext(context);

			HttpSession session = request.getSession(true);
			session.setAttribute(
					HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
					context);

			Usuario usuario = usuarioService.findUserByUsername(email);

			response.put("sessionId",    session.getId());
			response.put("id",           usuario.getId());
			response.put("nomeCompleto", usuario.getNomeCompleto());
			response.put("email",        usuario.getEmail());
			response.put("matricula",    usuario.getMatricula());
			response.put("curso",        usuario.getCurso());
			response.put("periodo",      usuario.getPeriodo());
			response.put("fotoPerfil",   usuario.getFotoPerfil());
			response.put("ativo",        usuario.getAtivo());
			response.put("role",         usuario.getRole());

			log.info("Login mobile bem-sucedido: {}", email);
			return ResponseEntity.ok(response);

		} catch (AuthenticationException e) {
			log.warn("Falha no login mobile para: {}", email);
			response.put("message", "E-mail ou senha inválidos.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
	}

	@PostMapping(value = "/register", produces = "application/json", consumes = "application/json")
	@ResponseBody
	public ResponseEntity<?> register(@RequestBody UsuarioDTO usuarioDTO) {
		Map<String, Object> response = new HashMap<>();

		try {
			log.info("=== RECEBENDO REQUISIÇÃO DE CADASTRO ===");
			log.info("Email: {}", usuarioDTO.email());
			log.info("Nome: {}", usuarioDTO.nomeCompleto());
			log.info("Matrícula: {}", usuarioDTO.matricula());

			// Validações
			if (usuarioDTO.nomeCompleto() == null || usuarioDTO.nomeCompleto().trim().isEmpty()) {
				response.put("message", "Nome completo é obrigatório");
				return ResponseEntity.badRequest().body(response);
			}

			if (usuarioDTO.email() == null || usuarioDTO.email().trim().isEmpty()) {
				response.put("message", "E-mail é obrigatório");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar e-mail institucional
			if (!usuarioDTO.email().matches("^[a-zA-Z0-9._%+-]+@fatec\\.sp\\.gov\\.br$")) {
				response.put("message", "Use um e-mail @fatec.sp.gov.br");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar senha
			if (usuarioDTO.senha() == null || usuarioDTO.senha().length() < 6) {
				response.put("message", "A senha deve ter no mínimo 6 caracteres");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar confirmação de senha
			if (usuarioDTO.confirmarSenha() == null
					|| !usuarioDTO.senha().equals(usuarioDTO.confirmarSenha())) {
				response.put("message", "As senhas não coincidem");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar matrícula
			if (usuarioDTO.matricula() == null || !usuarioDTO.matricula().matches("^[A-Za-z0-9]{8,15}$")) {
				response.put("message", "Matrícula deve ter entre 8 e 15 caracteres alfanuméricos");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar curso
			if (usuarioDTO.curso() == null || usuarioDTO.curso().trim().isEmpty()) {
				response.put("message", "Curso é obrigatório");
				return ResponseEntity.badRequest().body(response);
			}

			// Validar período
			if (usuarioDTO.periodo() == null) {
				response.put("message", "Período é obrigatório");
				return ResponseEntity.badRequest().body(response);
			}

			// Tentar cadastrar
			usuarioService.cadastrar(usuarioDTO);

			response.put("success", true);
			response.put("message", "Usuário cadastrado com sucesso!");
			log.info("Usuário cadastrado com sucesso: {}", usuarioDTO.email());
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