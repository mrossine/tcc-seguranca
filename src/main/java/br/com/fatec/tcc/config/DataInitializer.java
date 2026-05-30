package br.com.fatec.tcc.config;

import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(String... args) throws Exception {
		// Criar usuário admin se não existir
		if (!usuarioRepository.existsByEmail("admin@fatec.sp.gov.br")) {
			Usuario admin = new Usuario();
			admin.setNomeCompleto("Administrador");
			admin.setEmail("admin@fatec.sp.gov.br");
			admin.setSenha(passwordEncoder.encode("admin123"));
			admin.setMatricula("ADMIN001");
			admin.setCurso("Admininistrador");
			admin.setPeriodo(Usuario.Periodo.MANHA);
			admin.setRole(Usuario.Role.ADMIN);
			admin.setAtivo(true);
			usuarioRepository.save(admin);
			System.out.println("Usuário admin criado com sucesso!");
		}

		// Criar usuário moderador se não existir
		if (!usuarioRepository.existsByEmail("moderador@fatec.sp.gov.br")) {
			Usuario moderador = new Usuario();
			moderador.setNomeCompleto("Moderador");
			moderador.setEmail("moderador@fatec.sp.gov.br");
			moderador.setSenha(passwordEncoder.encode("moderador123"));
			moderador.setMatricula("MOD001");
			moderador.setCurso("Moderador");
			moderador.setPeriodo(Usuario.Periodo.TARDE);
			moderador.setRole(Usuario.Role.MODERATOR);
			moderador.setAtivo(true);
			usuarioRepository.save(moderador);
			System.out.println("Usuário moderador criado com sucesso!");
		}
	}
}