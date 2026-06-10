package br.com.fatec.tcc.controller;

import br.com.fatec.tcc.dto.UsuarioDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UsuarioService usuarioService;
	private final AuthenticationManager authenticationManager;

	@PostMapping(value = "/login", produces = "application/json", consumes = "application/json")
	public ResponseEntity<?> loginMobile(@RequestBody Map<String, String> credentials,
										 HttpServletRequest request) {
		String email = credentials.get("email");
		String senha = credentials.get("senha");

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

			log.info("Login mobile bem-sucedido: {}", email);
			return ResponseEntity.ok(Map.of(
				"sessionId",    session.getId(),
				"id",           usuario.getId(),
				"nomeCompleto", usuario.getNomeCompleto(),
				"email",        usuario.getEmail(),
				"matricula",    usuario.getMatricula(),
				"curso",        usuario.getCurso(),
				"periodo",      usuario.getPeriodo(),
				"fotoPerfil",   usuario.getFotoPerfil() != null ? usuario.getFotoPerfil() : "",
				"ativo",        usuario.getAtivo(),
				"role",         usuario.getRole()
			));

		} catch (AuthenticationException e) {
			log.warn("Falha no login mobile para: {}", email);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "E-mail ou senha inválidos."));
		}
	}

	@PostMapping(value = "/register", produces = "application/json", consumes = "application/json")
	public ResponseEntity<?> register(@RequestBody @Valid UsuarioDTO usuarioDTO,
									  BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			String erros = bindingResult.getFieldErrors().stream()
					.map(FieldError::getDefaultMessage)
					.collect(Collectors.joining("; "));
			return ResponseEntity.badRequest().body(Map.of("message", erros));
		}

		if (!usuarioDTO.senha().equals(usuarioDTO.confirmarSenha())) {
			return ResponseEntity.badRequest().body(Map.of("message", "As senhas não coincidem"));
		}

		try {
			usuarioService.cadastrar(usuarioDTO);
			log.info("Cadastro concluído com sucesso");
			return ResponseEntity.ok(Map.of("success", true, "message", "Usuário cadastrado com sucesso!"));
		} catch (RuntimeException e) {
			log.warn("Falha no cadastro: {}", e.getMessage());
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}

	@PostMapping(value = "/logout", produces = "application/json")
	public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request,
													  HttpServletResponse response,
													  Authentication auth) {
		new SecurityContextLogoutHandler().logout(request, response, auth);
		new CookieClearingLogoutHandler("JSESSIONID", "XSRF-TOKEN").logout(request, response, auth);
		log.info("Logout via API realizado");
		return ResponseEntity.ok(Map.of("message", "Logout realizado com sucesso"));
	}
}
