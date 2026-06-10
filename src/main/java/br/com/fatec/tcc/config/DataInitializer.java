package br.com.fatec.tcc.config;

import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final Environment environment;

	// Sem fallback: ausência da variável em produção deve falhar explicitamente
	@Value("${ADMIN_DEFAULT_PASSWORD:#{null}}")
	private String adminPassword;

	@Value("${MODERADOR_DEFAULT_PASSWORD:#{null}}")
	private String moderadorPassword;

	@Override
	public void run(String... args) throws Exception {
		boolean isProd = !Arrays.asList(environment.getActiveProfiles()).contains("dev");

		if (isProd) {
			if (adminPassword == null || adminPassword.isBlank()) {
				throw new IllegalStateException(
					"ADMIN_DEFAULT_PASSWORD não definida. Defina a variável de ambiente antes de iniciar em produção.");
			}
			if (moderadorPassword == null || moderadorPassword.isBlank()) {
				throw new IllegalStateException(
					"MODERADOR_DEFAULT_PASSWORD não definida. Defina a variável de ambiente antes de iniciar em produção.");
			}
		} else {
			if (adminPassword == null) adminPassword = "admin-dev-local";
			if (moderadorPassword == null) moderadorPassword = "moderador-dev-local";
		}

		if (!usuarioRepository.existsByEmail("admin@fatec.sp.gov.br")) {
			Usuario admin = new Usuario();
			admin.setNomeCompleto("Administrador");
			admin.setEmail("admin@fatec.sp.gov.br");
			admin.setSenha(passwordEncoder.encode(adminPassword));
			admin.setMatricula("ADMIN001");
			admin.setCurso("Administrador");
			admin.setPeriodo(Usuario.Periodo.MANHA);
			admin.setTipoUsuario(Usuario.TipoUsuario.FUNCIONARIO);
			admin.setRole(Usuario.Role.ADMIN);
			admin.setAtivo(true);
			usuarioRepository.save(admin);
			log.info("Usuário admin criado. Defina ADMIN_DEFAULT_PASSWORD em produção.");
		}

		if (!usuarioRepository.existsByEmail("moderador@fatec.sp.gov.br")) {
			Usuario moderador = new Usuario();
			moderador.setNomeCompleto("Moderador");
			moderador.setEmail("moderador@fatec.sp.gov.br");
			moderador.setSenha(passwordEncoder.encode(moderadorPassword));
			moderador.setMatricula("MOD001");
			moderador.setCurso("Moderador");
			moderador.setPeriodo(Usuario.Periodo.TARDE);
			moderador.setTipoUsuario(Usuario.TipoUsuario.FUNCIONARIO);
			moderador.setRole(Usuario.Role.MODERATOR);
			moderador.setAtivo(true);
			usuarioRepository.save(moderador);
			log.info("Usuário moderador criado. Defina MODERADOR_DEFAULT_PASSWORD em produção.");
		}
	}
}
