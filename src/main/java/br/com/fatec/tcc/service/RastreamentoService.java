package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.LocalizacaoDTO;
import br.com.fatec.tcc.dto.StatusCaronaDTO;
import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.repository.CaronaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de rastreamento em tempo real.
 *
 * Responsabilidades:
 *  1. Validar que quem envia é o motorista da carona
 *  2. Broadcast da localização para os passageiros via WebSocket
 *  3. Verificar geofencing (Fórmula de Haversine, raio de 50 m)
 *  4. Transicionar FECHADA → COMPLETADA quando motorista chega ao destino
 *  5. Agendar COMPLETADA → FINALIZADA após 10 minutos
 */
@Slf4j
@Service
public class RastreamentoService {

    private static final double RAIO_CHEGADA_METROS = 50.0;

    private static final String TOPIC_LOCALIZACAO = "/topic/carona/%d/localizacao";
    private static final String TOPIC_STATUS      = "/topic/carona/%d/status";

    private final SimpMessagingTemplate messagingTemplate;
    private final CaronaRepository caronaRepository;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final TransactionTemplate transactionTemplate;

    /**
     * Guarda IDs de caronas com finalização já agendada, evitando
     * agendar múltiplas vezes quando o motorista oscila no raio do destino.
     */
    private final Set<Long> caronasAgendadas = ConcurrentHashMap.newKeySet();

    public RastreamentoService(
            SimpMessagingTemplate messagingTemplate,
            CaronaRepository caronaRepository,
            @Qualifier("rastreamentoTaskScheduler") ThreadPoolTaskScheduler taskScheduler,
            PlatformTransactionManager transactionManager) {
        this.messagingTemplate   = messagingTemplate;
        this.caronaRepository    = caronaRepository;
        this.taskScheduler       = taskScheduler;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ponto de entrada — chamado pelo RastreamentoController a cada mensagem
    // ─────────────────────────────────────────────────────────────────────────

    public void processarLocalizacao(Long caronaId, LocalizacaoDTO loc, String emailMotorista) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new IllegalArgumentException("Carona não encontrada: " + caronaId));

        if (!carona.getMotorista().getEmail().equals(emailMotorista)) {
            log.warn("Usuário {} tentou enviar localização da carona {} sem ser motorista", emailMotorista, caronaId);
            return;
        }

        if (carona.getStatus() != Carona.StatusCarona.FECHADA) {
            log.debug("Carona {} ignorada para rastreamento — status={}", caronaId, carona.getStatus());
            return;
        }

        // 1. Broadcast para todos os passageiros inscritos no tópico
        messagingTemplate.convertAndSend(String.format(TOPIC_LOCALIZACAO, caronaId), loc);

        // 2. Geofencing — só verifica se as coordenadas do destino foram cadastradas
        if (carona.getDestinoLatitude() != null && carona.getDestinoLongitude() != null) {
            double distancia = haversineMetros(
                    loc.latitude(), loc.longitude(),
                    carona.getDestinoLatitude(), carona.getDestinoLongitude());

            log.debug("Carona {} — distância ao destino: {:.1f} m (raio: {} m)",
                    caronaId, distancia, RAIO_CHEGADA_METROS);

            if (distancia <= RAIO_CHEGADA_METROS && !caronasAgendadas.contains(caronaId)) {
                tentarMarcarComoCompletada(caronaId);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Geofencing: transição FECHADA → COMPLETADA
    // ─────────────────────────────────────────────────────────────────────────

    private void tentarMarcarComoCompletada(Long caronaId) {
        Boolean marcada = transactionTemplate.execute(status -> {
            Carona carona = caronaRepository.findById(caronaId).orElse(null);
            if (carona == null || carona.getStatus() != Carona.StatusCarona.FECHADA) {
                return false;
            }
            carona.setStatus(Carona.StatusCarona.COMPLETADA);
            caronaRepository.save(carona);
            return true;
        });

        if (!Boolean.TRUE.equals(marcada)) return;

        caronasAgendadas.add(caronaId);

        messagingTemplate.convertAndSend(
                String.format(TOPIC_STATUS, caronaId),
                new StatusCaronaDTO(caronaId, "COMPLETADA",
                        "Motorista chegou ao destino! A carona será finalizada em 10 minutos."));

        Instant momentoFinalizacao = Instant.now().plus(10, ChronoUnit.MINUTES);
        taskScheduler.schedule(() -> finalizarApos10Min(caronaId), momentoFinalizacao);

        log.info("Carona {} → COMPLETADA. Finalização automática agendada para {}", caronaId, momentoFinalizacao);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Timer: transição COMPLETADA → FINALIZADA após 10 minutos
    // ─────────────────────────────────────────────────────────────────────────

    private void finalizarApos10Min(Long caronaId) {
        try {
            transactionTemplate.execute(status -> {
                Carona carona = caronaRepository.findById(caronaId).orElse(null);
                if (carona == null) return null;

                if (carona.getStatus() != Carona.StatusCarona.COMPLETADA) {
                    log.info("Carona {} não está mais COMPLETADA ({}). Finalização automática cancelada.",
                            caronaId, carona.getStatus());
                    return null;
                }

                carona.setStatus(Carona.StatusCarona.FINALIZADA);
                caronaRepository.save(carona);
                log.info("Carona {} → FINALIZADA automaticamente após 10 minutos.", caronaId);
                return null;
            });

            messagingTemplate.convertAndSend(
                    String.format(TOPIC_STATUS, caronaId),
                    new StatusCaronaDTO(caronaId, "FINALIZADA",
                            "Carona finalizada! Você já pode avaliar o motorista."));

        } catch (Exception e) {
            log.error("Erro ao finalizar automaticamente a carona {}: {}", caronaId, e.getMessage(), e);
        } finally {
            caronasAgendadas.remove(caronaId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fórmula de Haversine — distância em metros entre dois pontos geográficos
    // ─────────────────────────────────────────────────────────────────────────

    public static double haversineMetros(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
