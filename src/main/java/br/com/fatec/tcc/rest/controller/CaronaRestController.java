package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.CaronaRequestDTO;
import br.com.fatec.tcc.dto.CaronaResponseDTO;
import br.com.fatec.tcc.dto.DenunciaRequestDTO;
import br.com.fatec.tcc.dto.MensagemCaronaDTO;
import br.com.fatec.tcc.dto.ParticipacaoCaronaDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.service.CaronaService;
import br.com.fatec.tcc.service.MensagemCaronaService;
import br.com.fatec.tcc.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/caronas")
@RequiredArgsConstructor
public class CaronaRestController {

    private final CaronaService caronaService;
    private final MensagemCaronaService mensagemService;
    private final UsuarioService usuarioService;

    /** GET /api/caronas — lista as caronas visíveis ao usuário logado. */
    @GetMapping
    public List<CaronaResponseDTO> listarCaronasDisponiveis(Authentication authentication) {
        return caronaService.listarCaronasDisponiveis(authentication.getName(), null, null, null, null);
    }

    /** POST /api/caronas — motorista oferece uma nova carona. */
    @PostMapping
    public ResponseEntity<CaronaResponseDTO> criarCarona(@RequestBody CaronaRequestDTO request,
                                                         Authentication authentication) {
        CaronaResponseDTO response = caronaService.oferecerCarona(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** GET /api/caronas/{id} — detalhes de uma carona. */
    @GetMapping("/{id}")
    public CaronaResponseDTO buscarCarona(@PathVariable Long id) {
        return caronaService.buscarPorId(id);
    }

    /** POST /api/caronas/{id}/solicitar — passageiro solicita vaga. */
    @PostMapping("/{id}/solicitar")
    public ResponseEntity<Map<String, Object>> solicitarVaga(@PathVariable Long id, Authentication auth) {
        try {
            caronaService.solicitarVaga(id, auth.getName());
            return ResponseEntity.ok(Map.of("message", "Solicitação enviada com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** GET /api/caronas/{id}/solicitacoes — solicitações pendentes (apenas o motorista). */
    @GetMapping("/{id}/solicitacoes")
    public List<ParticipacaoCaronaDTO> listarSolicitacoes(@PathVariable Long id, Authentication auth) {
        return caronaService.listarSolicitacoesPorCarona(id, auth.getName());
    }

    /** PUT .../aceitar — motorista aceita um passageiro. */
    @PutMapping("/{caronaId}/solicitacoes/{participacaoId}/aceitar")
    public ResponseEntity<Map<String, Object>> aceitarPassageiro(@PathVariable Long caronaId,
                                                                 @PathVariable Long participacaoId,
                                                                 Authentication auth) {
        try {
            caronaService.aceitarPassageiro(caronaId, participacaoId, auth.getName());
            return ResponseEntity.ok(Map.of("message", "Passageiro aceito com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** PUT .../recusar — motorista recusa um passageiro. */
    @PutMapping("/{caronaId}/solicitacoes/{participacaoId}/recusar")
    public ResponseEntity<Map<String, Object>> recusarPassageiro(@PathVariable Long caronaId,
                                                                 @PathVariable Long participacaoId,
                                                                 Authentication auth) {
        try {
            caronaService.recusarPassageiro(caronaId, participacaoId, auth.getName());
            return ResponseEntity.ok(Map.of("message", "Passageiro recusado"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** PUT /api/caronas/{id}/finalizar — motorista finaliza a viagem. */
    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Map<String, Object>> finalizarCarona(@PathVariable Long id, Authentication auth) {
        try {
            caronaService.finalizarCarona(id, auth.getName());
            return ResponseEntity.ok(Map.of("message", "Carona finalizada com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** PUT /api/caronas/{id}/cancelar — motorista cancela a carona. */
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Map<String, Object>> cancelarCarona(@PathVariable Long id, Authentication auth) {
        try {
            caronaService.cancelarCarona(id, auth.getName());
            return ResponseEntity.ok(Map.of("message", "Carona cancelada com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * POST /api/caronas/{id}/avaliar
     * Body: { "estrelas": 4, "comentario": "Ótima viagem!" }
     */
    @PostMapping("/{id}/avaliar")
    public ResponseEntity<Map<String, Object>> avaliarCarona(@PathVariable Long id,
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

    /** DELETE /api/caronas/{id}/excluir — exclui (admin) ou cancela (motorista) a carona. */
    @DeleteMapping("/{id}/excluir")
    public ResponseEntity<Map<String, Object>> excluirCarona(@PathVariable Long id, Authentication auth) {
        try {
            caronaService.excluirCarona(id, auth.getName());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * GET /api/caronas/{id}/passageiros-confirmados
     * Lista os passageiros confirmados — usado pelo motorista ao escolher quem denunciar.
     */
    @GetMapping("/{id}/passageiros-confirmados")
    public List<ParticipacaoCaronaDTO> passageirosConfirmados(@PathVariable Long id, Authentication auth) {
        return caronaService.listarPassageirosConfirmados(id, auth.getName());
    }

    /** PUT /api/caronas/{id}/finalizar-manual — motorista encerra do qualquer status não-terminal. */
    @PutMapping("/{id}/finalizar-manual")
    public ResponseEntity<Map<String, Object>> finalizarManual(@PathVariable Long id, Authentication auth) {
        try {
            caronaService.finalizarCaronaManualmente(id, auth.getName());
            return ResponseEntity.ok(Map.of("sucesso", true, "statusCarona", "FINALIZADA"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /** GET /api/caronas/{id}/mensagens — histórico de chat (motorista + passageiros confirmados). */
    @GetMapping("/{id}/mensagens")
    public ResponseEntity<?> listarMensagens(@PathVariable Long id, Authentication auth) {
        try {
            Usuario usuario = usuarioService.findUserByUsername(auth.getName());
            List<MensagemCaronaDTO> msgs = mensagemService.listarMensagens(id, usuario);
            return ResponseEntity.ok(msgs);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    /** POST /api/caronas/{id}/mensagens — envia mensagem no chat da carona. */
    @PostMapping("/{id}/mensagens")
    public ResponseEntity<?> enviarMensagem(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            Authentication auth) {
        try {
            String conteudo = body.get("conteudo");
            Usuario usuario = usuarioService.findUserByUsername(auth.getName());
            MensagemCaronaDTO dto = mensagemService.enviarMensagem(id, usuario, conteudo);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * POST /api/caronas/{id}/denunciar
     * Body: { "categoria": "...", "descricao": "...", "alvoEmail": "...", "todaCarona": false }
     */
    @PostMapping("/{id}/denunciar")
    public ResponseEntity<Map<String, Object>> denunciar(@PathVariable Long id,
                                                         @RequestBody DenunciaRequestDTO body,
                                                         Authentication auth) {
        try {
            caronaService.denunciar(id, auth.getName(), body);
            return ResponseEntity.ok(Map.of("message", "Denúncia registrada com sucesso!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
