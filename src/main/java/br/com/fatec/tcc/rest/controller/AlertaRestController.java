package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.AlertaResponseDTO;
import br.com.fatec.tcc.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AlertaRestController {

    private final AlertaService alertaService;

    @GetMapping("/alertas")
    public List<AlertaResponseDTO> listarAlertas(Authentication authentication) {
        String email = authentication.getName();
        return alertaService.listarAlertasAtivos(email);
    }

    /**
     * Exclusão de alerta via DELETE /api/alertas/{id}
     */
    @DeleteMapping("/alertas/{id}")
    public ResponseEntity<?> excluirAlerta(@PathVariable Long id, Authentication authentication) {
        try {
            alertaService.removerAlerta(id, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Alerta excluído com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }
    }
}