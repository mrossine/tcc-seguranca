package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.DenunciaAdminDTO;
import br.com.fatec.tcc.dto.UsuarioAdminDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.service.CaronaService;
import br.com.fatec.tcc.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRestController {

    private final UsuarioService usuarioService;
    private final CaronaService caronaService;
    private final JdbcTemplate jdbcTemplate;

    // 1. Listar todos os usuários — retorna UsuarioAdminDTO com ID incluído
    @GetMapping("/usuarios")
    public Page<UsuarioAdminDTO> listarUsuariosPaginado(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String curso,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Usuario> usuarios = usuarioService.listarUsuariosEntidadePaginado(nome, email, curso, pageable);
        return usuarios.map(usuarioService::convertToAdminDTO);
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

    // 4. Listar denúncias das caronas (opcionalmente filtradas por status)
    @GetMapping("/denuncias")
    public List<DenunciaAdminDTO> listarDenuncias(@RequestParam(required = false) String status) {
        return caronaService.listarDenuncias(status);
    }

    // 5. Atualizar o status de uma denúncia
    @PutMapping("/denuncias/{id}/status")
    public ResponseEntity<?> atualizarStatusDenuncia(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body) {
        try {
            caronaService.atualizarStatusDenuncia(id, body.get("status"));
            return ResponseEntity.ok().body(Map.of("message", "Status atualizado com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}