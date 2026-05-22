package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.EstatisticasDTO;
import br.com.fatec.tcc.model.Alerta;
import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.AlertaRepository;
import br.com.fatec.tcc.repository.CaronaRepository;
import br.com.fatec.tcc.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstatisticaService {

    private final AlertaRepository alertaRepository;
    private final CaronaRepository caronaRepository;
    private final UsuarioRepository usuarioRepository;

    public EstatisticasDTO getEstatisticas() {
        try {
            // Total de usuários
            Long totalUsuarios = usuarioRepository.count();
            log.debug("Total de usuários: {}", totalUsuarios);

            // Total de alertas ativos
            List<Alerta> alertasAtivos = alertaRepository.findByStatusOrderByDataCriacaoDesc(Alerta.StatusAlerta.ATIVO);
            Long totalAlertasAtivos = (long) alertasAtivos.size();
            log.debug("Total de alertas ativos: {}", totalAlertasAtivos);

            // Total de caronas disponíveis
            List<Carona> caronasDisponiveis = caronaRepository.findByStatusAndHorarioSaidaAfter(
                    Carona.StatusCarona.ABERTA, LocalDateTime.now());
            Long totalCaronasDisponiveis = (long) caronasDisponiveis.size();
            log.debug("Total de caronas disponíveis: {}", totalCaronasDisponiveis);

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

            // Retorna o record com todos os valores
            return new EstatisticasDTO(
                    totalUsuarios,
                    totalAlertasAtivos,
                    totalCaronasDisponiveis,
                    alertasPorTipoMap,
                    alertasPorHoraMap
            );

        } catch (Exception e) {
            log.error("Erro ao carregar estatísticas: {}", e.getMessage(), e);
            // Retorna record com valores padrão (vazios) em caso de erro
            return new EstatisticasDTO(0L, 0L, 0L, new HashMap<>(), new HashMap<>());
        }
    }

    public Map<String, Long> contarUsuariosPorPeriodo() {
        List<Object[]> results = usuarioRepository.countByPeriodo();
        return results.stream()
                .collect(Collectors.toMap(
                        row -> ((Usuario.Periodo) row[0]).name(),
                        row -> (Long) row[1]
                ));
    }
}