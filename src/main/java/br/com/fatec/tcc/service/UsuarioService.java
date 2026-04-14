package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.UsuarioDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		log.debug("Buscando usuário por email: {}", email);
		return usuarioRepository.findByEmail(email).orElseThrow(() -> {
			log.error("Usuário não encontrado: {}", email);
			return new UsernameNotFoundException("Usuário não encontrado: " + email);
		});
	}

	public Usuario findUserByUsername(String email) {
		return usuarioRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + email));
	}

	@Transactional
	public Usuario cadastrar(UsuarioDTO usuarioDTO) {
		log.info("=== INICIANDO CADASTRO ===");
		log.info("Email: {}", usuarioDTO.getEmail());
		log.info("Matrícula: {}", usuarioDTO.getMatricula());

		// Verificar se email já existe
		if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
			log.warn("Email já cadastrado: {}", usuarioDTO.getEmail());
			throw new RuntimeException("E-mail já cadastrado");
		}

		// Verificar se matrícula já existe
		if (usuarioRepository.existsByMatricula(usuarioDTO.getMatricula())) {
			log.warn("Matrícula já cadastrada: {}", usuarioDTO.getMatricula());
			throw new RuntimeException("Matrícula já cadastrada");
		}

		Usuario usuario = new Usuario();
		usuario.setNomeCompleto(usuarioDTO.getNomeCompleto());
		usuario.setEmail(usuarioDTO.getEmail());
		usuario.setSenha(passwordEncoder.encode(usuarioDTO.getSenha()));
		usuario.setMatricula(usuarioDTO.getMatricula());
		usuario.setCurso(usuarioDTO.getCurso());
		usuario.setPeriodo(usuarioDTO.getPeriodo());
		usuario.setAtivo(true);
		usuario.setRole(Usuario.Role.USER);

		Usuario saved = usuarioRepository.save(usuario);
		log.info("Usuário salvo com sucesso! ID: {}", saved.getId());

		return saved;
	}

	@Transactional
	public Usuario atualizarPerfil(Long id, UsuarioDTO usuarioDTO) {
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

		usuario.setNomeCompleto(usuarioDTO.getNomeCompleto());
		usuario.setCurso(usuarioDTO.getCurso());
		usuario.setPeriodo(usuarioDTO.getPeriodo());

		if (usuarioDTO.getFotoPerfil() != null) {
			usuario.setFotoPerfil(usuarioDTO.getFotoPerfil());
		}

		return usuarioRepository.save(usuario);
	}

	@Transactional
	public void alterarSenha(Long id, String senhaAtual, String novaSenha) {
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

		if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
			throw new RuntimeException("Senha atual incorreta");
		}

		usuario.setSenha(passwordEncoder.encode(novaSenha));
		usuarioRepository.save(usuario);
	}

	public Usuario buscarPorId(Long id) {
		return usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
	}

	public boolean checkEmailExists(String email) {
		return usuarioRepository.existsByEmail(email);
	}
}