package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.UsuarioAdminDTO;
import br.com.fatec.tcc.dto.UsuarioDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.UsuarioRepository;
import br.com.fatec.tcc.specification.UsuarioSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.DisabledException;
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
        log.debug("Autenticando usuário");
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        if (alunoExpirado(usuario)) {
            throw new DisabledException("Matrícula expirada. Entre em contato com a secretaria.");
        }
        return usuario;
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
        usuario.setTipoUsuario(usuarioDTO.tipoUsuario());
        usuario.setAtivo(true);
        usuario.setRole(Usuario.Role.USER);

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuário salvo com ID: {}", saved.getId());
        return saved;
    }

    // Somente o nome completo pode ser alterado pelo próprio usuário.
    // Curso e período derivam da matrícula e não são editáveis.
    @Transactional
    public Usuario atualizarPerfil(Long id, UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        usuario.setNomeCompleto(usuarioDTO.nomeCompleto());
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
        if (novaSenha == null || novaSenha.length() < 8) {
            throw new RuntimeException("A nova senha deve ter no mínimo 8 caracteres");
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

    // Lista usuários com paginação e filtros — retorna entidades (com ID) para uso no admin
    public Page<Usuario> listarUsuariosEntidadePaginado(String nome, String email, String curso, Pageable pageable) {
        return usuarioRepository.findAll(
                UsuarioSpecification.filtrar(nome, email, curso),
                pageable
        );
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

    // Converte Usuario para UsuarioAdminDTO (inclui ID para administração)
    public UsuarioAdminDTO convertToAdminDTO(Usuario u) {
        return new UsuarioAdminDTO(
                u.getId(),
                u.getNomeCompleto(),
                u.getEmail(),
                u.getMatricula(),
                u.getCurso(),
                u.getFotoPerfil()
        );
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
                u.getTipoUsuario(),
                u.getFotoPerfil()
        );
    }

    // Regra de validade do aluno (5 anos a partir do ano de ingresso na matrícula)

    /** Tempo máximo, em anos, que um aluno pode permanecer ativo (duração máxima dos cursos da Fatec). */
    private static final int ANOS_MAX_ALUNO = 5;

    /**
     * Extrai o ano de ingresso da matrícula procurando um ano plausível (20xx).
     * Retorna null quando não é possível determinar (ex.: matrículas sem ano).
     */
    public static Integer extrairAnoIngresso(String matricula) {
        if (matricula == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20\\d{2})").matcher(matricula);
        int anoAtual = java.time.Year.now().getValue();
        while (m.find()) {
            int ano = Integer.parseInt(m.group(1));
            if (ano >= 2000 && ano <= anoAtual) {
                return ano;
            }
        }
        return null;
    }

    /** Indica se um aluno já ultrapassou o tempo máximo de 5 anos desde o ingresso. */
    public static boolean alunoExpirado(Usuario u) {
        if (u.getTipoUsuario() != Usuario.TipoUsuario.ALUNO) return false;
        if (u.getRole() == Usuario.Role.ADMIN || u.getRole() == Usuario.Role.MODERATOR) return false;
        Integer anoIngresso = extrairAnoIngresso(u.getMatricula());
        if (anoIngresso == null) return false;
        return (java.time.Year.now().getValue() - anoIngresso) > ANOS_MAX_ALUNO;
    }

    /**
     * Desativa todos os alunos cujo prazo de 5 anos expirou (não poderão mais logar).
     * Retorna quantos foram desativados.
     */
    @Transactional
    public int desativarAlunosExpirados() {
        List<Usuario> ativos = usuarioRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .filter(UsuarioService::alunoExpirado)
                .collect(Collectors.toList());
        for (Usuario u : ativos) {
            u.setAtivo(false);
        }
        if (!ativos.isEmpty()) {
            usuarioRepository.saveAll(ativos);
            log.info("Desativados {} aluno(s) por excederem o prazo de {} anos", ativos.size(), ANOS_MAX_ALUNO);
        }
        return ativos.size();
    }
}