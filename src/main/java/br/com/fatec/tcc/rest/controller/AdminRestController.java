package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.DenunciaAdminDTO;
import br.com.fatec.tcc.dto.DenunciaAlertaAdminDTO;
import br.com.fatec.tcc.dto.ModeloCustomDTO;
import br.com.fatec.tcc.dto.UsuarioAdminDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.service.AlertaService;
import br.com.fatec.tcc.service.CaronaService;
import br.com.fatec.tcc.service.UsuarioService;
import br.com.fatec.tcc.service.VeiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST administrativa. Todos os métodos exigem o papel ADMIN
 * (graças ao @PreAuthorize na classe). Fornece dados para as telas de admin:
 * usuários, total via stored procedure e denúncias das caronas.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRestController {

    private final UsuarioService usuarioService;
    private final CaronaService caronaService;
    private final AlertaService alertaService;
    private final VeiculoService veiculoService;
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
    public ResponseEntity<Map<String, Object>> deletarUsuario(@PathVariable Long id) {
        try {
            usuarioService.deletarUsuario(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Stored procedure: total de usuários
    @GetMapping("/sp-total-usuarios")
    public ResponseEntity<?> chamarProcedureTotalUsuarios() {
        try {
            Long total = jdbcTemplate.queryForObject("CALL sp_total_usuarios()", (rs, rowNum) -> rs.getLong("total"));
            return ResponseEntity.ok(Map.of("total", total != null ? total : 0L));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao executar stored procedure: " + e.getMessage()));
        }
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

    // 6. Listar denúncias de ALERTAS (categoria separada das de carona)
    @GetMapping("/denuncias-alertas")
    public List<DenunciaAlertaAdminDTO> listarDenunciasAlertas(@RequestParam(required = false) String status) {
        return alertaService.listarDenunciasAlerta(status);
    }

    // 7. Atualizar o status de uma denúncia de alerta
    @PutMapping("/denuncias-alertas/{id}/status")
    public ResponseEntity<?> atualizarStatusDenunciaAlerta(@PathVariable Long id,
                                                           @RequestBody Map<String, String> body) {
        try {
            alertaService.atualizarStatusDenunciaAlerta(id, body.get("status"));
            return ResponseEntity.ok().body(Map.of("message", "Status atualizado com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 8. Listar solicitações de modelos customizados
    @GetMapping("/modelos-custom")
    public List<ModeloCustomDTO> listarModelosCustom(@RequestParam(required = false) String status) {
        return veiculoService.listarModelosCustom(status);
    }

    // 9. Aprovar ou rejeitar um modelo customizado
    @PutMapping("/modelos-custom/{id}")
    public ResponseEntity<?> resolverModeloCustom(@PathVariable Long id,
                                                  @RequestBody Map<String, String> body) {
        try {
            ModeloCustomDTO dto = veiculoService.resolverModeloCustom(
                    id, body.get("decisao"), body.getOrDefault("observacao", ""));
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}