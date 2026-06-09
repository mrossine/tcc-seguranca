package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.AlertaRequestDTO;
import br.com.fatec.tcc.dto.AlertaResponseDTO;
import br.com.fatec.tcc.dto.DenunciaAlertaRequestDTO;
import br.com.fatec.tcc.model.AlertaReacao;
import br.com.fatec.tcc.service.AlertaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaRestController {

    private final AlertaService alertaService;

    @GetMapping
    public List<AlertaResponseDTO> listarAlertas(Authentication authentication) {
        return alertaService.listarAlertasAtivos(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<AlertaResponseDTO> criarAlerta(@RequestBody @Valid AlertaRequestDTO request,
                                                         Authentication authentication) {
        AlertaResponseDTO response = alertaService.criarAlerta(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Map<String, Object>> confirmarAlerta(@PathVariable Long id) {
        alertaService.confirmarAlerta(id);
        return ResponseEntity.ok(Map.of("message", "Alerta confirmado com sucesso"));
    }

    /** POST /api/alertas/{id}/denunciar — Body: { "categoria": "...", "justificativa": "..." }. */
    @PostMapping("/{id}/denunciar")
    public ResponseEntity<Map<String, Object>> denunciarAlerta(@PathVariable Long id,
                                                              @RequestBody DenunciaAlertaRequestDTO body,
                                                              Authentication authentication) {
        try {
            alertaService.denunciarAlerta(id, authentication.getName(), body.categoria(), body.justificativa());
            return ResponseEntity.ok(Map.of("message", "Denúncia registrada com sucesso! A administração irá analisar."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** POST /api/alertas/{id}/reagir — LIKE ou DISLIKE (toggle: mesmo tipo remove a reação). */
    @PostMapping("/{id}/reagir")
    public ResponseEntity<Map<String, Object>> reagirAlerta(@PathVariable Long id,
                                                            @RequestBody Map<String, String> body,
                                                            Authentication authentication) {
        try {
            String tipoStr = body.get("tipo");
            AlertaReacao.TipoReacao tipo = AlertaReacao.TipoReacao.valueOf(tipoStr.toUpperCase());
            Map<String, Long> contadores = alertaService.reagirAlerta(id, tipo, authentication.getName());
            return ResponseEntity.ok(Map.of(
                "sucesso", true,
                "likes",    contadores.get("likes"),
                "dislikes", contadores.get("dislikes")
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tipo inválido. Use LIKE ou DISLIKE"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    /** GET /api/alertas/{id}/reacoes — contadores de likes e dislikes. */
    @GetMapping("/{id}/reacoes")
    public ResponseEntity<Map<String, Long>> obterReacoes(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(alertaService.obterContadoresReacoes(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> excluirAlerta(@PathVariable Long id,
                                                             Authentication authentication) {
        try {
            alertaService.removerAlerta(id, authentication.getName());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
