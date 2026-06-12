package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.AlertaResponseDTO;
import br.com.fatec.tcc.dto.CaronaResponseDTO;
import br.com.fatec.tcc.dto.UsuarioDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.service.AlertaService;
import br.com.fatec.tcc.service.CaronaService;
import br.com.fatec.tcc.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuario")
@RequiredArgsConstructor
public class UsuarioRestController {

    private final UsuarioService usuarioService;
    private final AlertaService alertaService;
    private final CaronaService caronaService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication auth) {
        Usuario u = usuarioService.findUserByUsername(auth.getName());
        Map<String, Object> response = new HashMap<>();
        response.put("id",           u.getId());
        response.put("nomeCompleto", u.getNomeCompleto());
        response.put("email",        u.getEmail());
        response.put("matricula",    u.getMatricula());
        response.put("curso",        u.getCurso());
        response.put("periodo",      u.getPeriodo());
        response.put("fotoPerfil",   u.getFotoPerfil());
        response.put("dataCadastro", u.getDataCadastro());
        response.put("ativo",        u.getAtivo());
        response.put("role",         u.getRole());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/perfil")
    public ResponseEntity<Map<String, Object>> atualizarPerfil(@RequestBody Map<String, String> body,
                                                               Authentication auth) {
        String nomeCompleto = body.get("nomeCompleto");
        if (nomeCompleto == null || nomeCompleto.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nome completo é obrigatório"));
        }
        Usuario usuario = usuarioService.findUserByUsername(auth.getName());
        UsuarioDTO dto = new UsuarioDTO(
                nomeCompleto.trim(),
                usuario.getEmail(),
                null, null,
                usuario.getMatricula(),
                usuario.getCurso(),
                usuario.getPeriodo(),
                usuario.getTipoUsuario(),
                usuario.getFotoPerfil()
        );
        usuarioService.atualizarPerfil(usuario.getId(), dto);
        return ResponseEntity.ok(Map.of("message", "Perfil atualizado com sucesso"));
    }

    @PutMapping("/senha")
    public ResponseEntity<Map<String, Object>> alterarSenha(@RequestBody Map<String, String> body,
                                                            Authentication auth) {
        String senhaAtual = body.get("senhaAtual");
        String novaSenha = body.get("novaSenha");
        if (senhaAtual == null || senhaAtual.isBlank() || novaSenha == null || novaSenha.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Senha atual e nova senha são obrigatórias"));
        }
        if (novaSenha.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "A nova senha deve ter no mínimo 8 caracteres"));
        }
        try {
            Usuario usuario = usuarioService.findUserByUsername(auth.getName());
            usuarioService.alterarSenha(usuario.getId(), senhaAtual, novaSenha);
            return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/historico/alertas")
    public List<AlertaResponseDTO> historicoAlertas(Authentication auth) {
        Usuario usuario = usuarioService.findUserByUsername(auth.getName());
        return alertaService.listarAlertasPorUsuario(usuario);
    }

    @GetMapping("/historico/caronas")
    public List<CaronaResponseDTO> historicoCaronas(Authentication auth) {
        Usuario usuario = usuarioService.findUserByUsername(auth.getName());
        return caronaService.listarCaronasPorUsuario(usuario);
    }
}
