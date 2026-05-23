package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.UsuarioDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.UsuarioRepository;
import br.com.fatec.tcc.specification.UsuarioSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    //Autenticação e busca
    //Método obrigatório do Spring Security. Busca usuário por e-mail.
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Buscando usuário por email: {}", email);
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }

    //Busca um usuário completo (entidade) pelo e-mail – útil para controllers.
    public Usuario findUserByUsername(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + email));
    }

    //CRUD básico
    //Cadastra um novo usuário comum (role USER).
    //Valida duplicidade de e-mail e matrícula.
    @Transactional
    public Usuario cadastrar(UsuarioDTO usuarioDTO) {
        log.info("=== INICIANDO CADASTRO ===");
        log.info("Email: {}", usuarioDTO.email());
        log.info("Matrícula: {}", usuarioDTO.matricula());

        if (usuarioRepository.existsByEmail(usuarioDTO.email())) {
            throw new RuntimeException("E-mail já cadastrado");
        }
        if (usuarioRepository.existsByMatricula(usuarioDTO.matricula())) {
            throw new RuntimeException("Matrícula já cadastrada");
        }

        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(usuarioDTO.nomeCompleto());
        usuario.setEmail(usuarioDTO.email());
        usuario.setSenha(passwordEncoder.encode(usuarioDTO.senha()));
        usuario.setMatricula(usuarioDTO.matricula());
        usuario.setCurso(usuarioDTO.curso());
        usuario.setPeriodo(usuarioDTO.periodo());
        usuario.setAtivo(true);
        usuario.setRole(Usuario.Role.USER);

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuário salvo com ID: {}", saved.getId());
        return saved;
    }

    //Atualiza os dados do perfil (nome, curso, período, foto).
    @Transactional
    public Usuario atualizarPerfil(Long id, UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        usuario.setNomeCompleto(usuarioDTO.nomeCompleto());
        usuario.setCurso(usuarioDTO.curso());
        usuario.setPeriodo(usuarioDTO.periodo());
        if (usuarioDTO.fotoPerfil() != null) {
            usuario.setFotoPerfil(usuarioDTO.fotoPerfil());
        }
        return usuarioRepository.save(usuario);
    }

    //Altera a senha do usuário, verificando a senha atual.
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

    //Busca um usuário pelo ID (entidade JPA).
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    //Verifica se um e-mail já está cadastrado.
    public boolean checkEmailExists(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    //Administração (listagem, exclusão, relatórios)
    //Retorna todos os usuários como DTO (sem senha). Utilizado para administração.
    public List<UsuarioDTO> listarTodosUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    //Exclui fisicamente um usuário pelo ID.
    @Transactional
    public void deletarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        usuarioRepository.delete(usuario);
    }

    //Lista usuários com paginação e filtros dinâmicos (nome, email, curso).
    //Retorna uma página de UsuarioDTO.
    public Page<UsuarioDTO> listarUsuariosPaginado(String nome, String email, String curso, Pageable pageable) {
        Page<Usuario> page = usuarioRepository.findAll(
                UsuarioSpecification.filtrar(nome, email, curso),
                pageable
        );
        return page.map(this::convertToDTO);
    }

    //Métodos auxiliares
    //Converte uma entidade Usuario em UsuarioDTO (inclui ID, exclui senha).
    private UsuarioDTO convertToDTO(Usuario u) {
        return new UsuarioDTO(
                u.getNomeCompleto(),
                u.getEmail(),
                null,          // senha
                null,          // confirmarSenha
                u.getMatricula(),
                u.getCurso(),
                u.getPeriodo(),
                u.getFotoPerfil()
        );
    }
}