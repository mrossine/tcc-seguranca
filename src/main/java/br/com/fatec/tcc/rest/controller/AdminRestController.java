package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.UsuarioAdminDTO;
import br.com.fatec.tcc.dto.UsuarioDTO;
import br.com.fatec.tcc.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRestController {

    private final UsuarioService usuarioService;
    private final JdbcTemplate jdbcTemplate;

    // 1. Listar todos os usuários
    @GetMapping("/usuarios")
    public Page<UsuarioAdminDTO> listarUsuariosPaginado(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String curso,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<UsuarioDTO> usuariosDTO = usuarioService.listarUsuariosPaginado(nome, email, curso, pageable);
        return usuariosDTO.map(dto -> new UsuarioAdminDTO(
                null, // ID será adicionado abaixo
                dto.nomeCompleto(),
                dto.email(),
                dto.matricula(),
                dto.curso(),
                dto.fotoPerfil()
        ));
    }

    // 2. Deletar usuário por ID
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> deletarUsuario(@PathVariable Long id) {
        try {
            usuarioService.deletarUsuario(id);
            return ResponseEntity.ok().body(Map.of("message", "Usuário excluído com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Stored procedure: total de usuários
    @GetMapping("/sp-total-usuarios")
    public Map<String, Long> chamarProcedureTotalUsuarios() {
        Long total = jdbcTemplate.queryForObject("CALL sp_total_usuarios()", (rs, rowNum) -> rs.getLong("total"));
        return Map.of("total", total);
    }
}