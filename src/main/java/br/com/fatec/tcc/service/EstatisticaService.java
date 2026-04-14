package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.EstatisticasDTO;
import br.com.fatec.tcc.model.Alerta;
import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.repository.AlertaRepository;
import br.com.fatec.tcc.repository.CaronaRepository;
import br.com.fatec.tcc.repository.ParticipacaoCaronaRepository;
import br.com.fatec.tcc.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstatisticaService {
    
    private final AlertaRepository alertaRepository;
    private final CaronaRepository caronaRepository;
    private final UsuarioRepository usuarioRepository;

    public EstatisticasDTO getEstatisticas() {
        EstatisticasDTO dto = new EstatisticasDTO();
        
        try {
            // Total de usuários
            dto.setTotalUsuarios(usuarioRepository.count());
            log.debug("Total de usuários: {}", dto.getTotalUsuarios());
            
            // Total de alertas ativos
            List<Alerta> alertasAtivos = alertaRepository.findByStatusOrderByDataCriacaoDesc(Alerta.StatusAlerta.ATIVO);
            dto.setTotalAlertasAtivos((long) alertasAtivos.size());
            log.debug("Total de alertas ativos: {}", dto.getTotalAlertasAtivos());
            
            // Total de caronas disponíveis
            List<Carona> caronasDisponiveis = caronaRepository.findByStatusAndHorarioSaidaAfter(
                Carona.StatusCarona.ABERTA, LocalDateTime.now());
            dto.setTotalCaronasDisponiveis((long) caronasDisponiveis.size());
            log.debug("Total de caronas disponíveis: {}", dto.getTotalCaronasDisponiveis());
            
            // Alertas por tipo
            List<Object[]> alertasPorTipo = alertaRepository.countByTipo();
            Map<String, Long> alertasPorTipoMap = new HashMap<>();
            if (alertasPorTipo != null) {
                for (Object[] row : alertasPorTipo) {
                    if (row != null && row.length >= 2) {
                        String tipo = row[0] != null ? row[0].toString() : "NÃO_IDENTIFICADO";
                        Long quantidade = row[1] != null ? (Long) row[1] : 0L;
                        alertasPorTipoMap.put(tipo, quantidade);
                    }
                }
            }
            dto.setAlertasPorTipo(alertasPorTipoMap);
            
            // Alertas por hora
            List<Object[]> alertasPorHora = alertaRepository.countByHour();
            Map<Integer, Long> alertasPorHoraMap = new HashMap<>();
            if (alertasPorHora != null) {
                for (Object[] row : alertasPorHora) {
                    if (row != null && row.length >= 2) {
                        Integer hora = row[0] != null ? ((Number) row[0]).intValue() : 0;
                        Long quantidade = row[1] != null ? (Long) row[1] : 0L;
                        alertasPorHoraMap.put(hora, quantidade);
                    }
                }
            }
            dto.setAlertasPorHora(alertasPorHoraMap);
            
        } catch (Exception e) {
            log.error("Erro ao carregar estatísticas: {}", e.getMessage(), e);
            // Retorna DTO com valores padrão em caso de erro
            dto.setTotalUsuarios(0L);
            dto.setTotalAlertasAtivos(0L);
            dto.setTotalCaronasDisponiveis(0L);
            dto.setAlertasPorTipo(new HashMap<>());
            dto.setAlertasPorHora(new HashMap<>());
        }
        
        return dto;
    }
}