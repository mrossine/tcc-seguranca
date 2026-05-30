package br.com.fatec.tcc.websocket;

import br.com.fatec.tcc.dto.LocalizacaoDTO;
import br.com.fatec.tcc.service.RastreamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Controller WebSocket (STOMP).
 *
 * Motorista envia para:      /app/carona/{caronaId}/localizacao
 * Passageiros recebem em:    /topic/carona/{caronaId}/localizacao
 * Status é publicado em:     /topic/carona/{caronaId}/status
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RastreamentoController {

    private final RastreamentoService rastreamentoService;

    @MessageMapping("/carona/{caronaId}/localizacao")
    public void receberLocalizacao(
            @DestinationVariable Long caronaId,
            @Payload LocalizacaoDTO payload,
            Principal principal) {

        if (principal == null) {
            log.warn("Mensagem WebSocket sem autenticação para carona {}", caronaId);
            return;
        }

        LocalizacaoDTO localizacao = new LocalizacaoDTO(caronaId, payload.latitude(), payload.longitude());

        log.debug("Localização — carona={} motorista={} lat={} lng={}",
                caronaId, principal.getName(), payload.latitude(), payload.longitude());

        rastreamentoService.processarLocalizacao(caronaId, localizacao, principal.getName());
    }
}
