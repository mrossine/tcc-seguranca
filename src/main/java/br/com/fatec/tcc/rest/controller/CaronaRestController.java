package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.CaronaResponseDTO;
import br.com.fatec.tcc.dto.ParticipacaoCaronaDTO;
import br.com.fatec.tcc.service.CaronaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/caronas")
@RequiredArgsConstructor
public class CaronaRestController {

    private final CaronaService caronaService;

    @GetMapping
    public List<CaronaResponseDTO> listarCaronasDisponiveis(Authentication authentication) {
        return caronaService.listarCaronasDisponiveis(authentication.getName(), null, null, null, null);
    }

    @GetMapping("/{id}")
    public CaronaResponseDTO buscarCarona(@PathVariable Long id) {
        return caronaService.buscarPorId(id);
    }

    @PostMapping("/{id}/solicitar")
    public ResponseEntity<?> solicitarVaga(@PathVariable Long id, Authentication auth) {
        try {
            caronaService.solicitarVaga(id, auth.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/solicitacoes")
    public List<ParticipacaoCaronaDTO> listarSolicitacoes(@PathVariable Long id, Authentication auth) {
        return caronaService.listarSolicitacoesPorCarona(id, auth.getName());
    }

    @PutMapping("/{caronaId}/solicitacoes/{participacaoId}/aceitar")
    public ResponseEntity<?> aceitarPassageiro(@PathVariable Long caronaId,
                                               @PathVariable Long participacaoId,
                                               Authentication auth) {
        try {
            caronaService.aceitarPassageiro(caronaId, participacaoId, auth.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{caronaId}/solicitacoes/{participacaoId}/recusar")
    public ResponseEntity<?> recusarPassageiro(@PathVariable Long caronaId,
                                               @PathVariable Long participacaoId,
                                               Authentication auth) {
        try {
            caronaService.recusarPassageiro(caronaId, participacaoId, auth.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<?> finalizarCarona(@PathVariable Long id, Authentication auth) {
        try {
            caronaService.finalizarCarona(id, auth.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarCarona(@PathVariable Long id, Authentication auth) {
        try {
            caronaService.cancelarCarona(id, auth.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * POST /api/caronas/{id}/avaliar
     * Body: { "estrelas": 4, "comentario": "Ótima viagem!" }
     */
    @PostMapping("/{id}/avaliar")
    public ResponseEntity<?> avaliarCarona(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           Authentication auth) {
        try {
            Integer estrelas = (Integer) body.get("estrelas");
            String comentario = (String) body.getOrDefault("comentario", null);
            caronaService.avaliarCarona(id, auth.getName(), estrelas, comentario);
            return ResponseEntity.ok(Map.of("message", "Avaliação registrada com sucesso!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/excluir")
    public ResponseEntity<?> excluirCarona(@PathVariable Long id, Authentication auth) {
        try {
            caronaService.excluirCarona(id, auth.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}