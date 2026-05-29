package br.com.fatec.tcc.scheduler;

import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.repository.CaronaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaronaScheduler {

    private final CaronaRepository caronaRepository;

    /**
     * Executa a cada minuto. Marca como FECHADA as caronas ABERTA/CHEIA
     * cujo horário de saída já passou.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void fecharCaronasVencidas() {
        List<Carona> vencidas = caronaRepository.findCaronasParaFechar(LocalDateTime.now());
        if (!vencidas.isEmpty()) {
            log.info("Fechando {} carona(s) com horário vencido", vencidas.size());
            for (Carona carona : vencidas) {
                carona.setStatus(Carona.StatusCarona.FECHADA);
                caronaRepository.save(carona);
            }
        }
    }
}
