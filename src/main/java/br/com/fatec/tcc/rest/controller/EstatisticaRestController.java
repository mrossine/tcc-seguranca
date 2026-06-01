package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.EstatisticasDTO;
import br.com.fatec.tcc.service.EstatisticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API REST das estatísticas — consumida por gráficos/JS via fetch.
 */
@RestController
@RequestMapping("/api/estatisticas")
@RequiredArgsConstructor
public class EstatisticaRestController {

    private final EstatisticaService estatisticaService;

    /** GET /api/estatisticas/ocorrencias — devolve o relatório completo em JSON. */
    @GetMapping("/ocorrencias")
    public EstatisticasDTO getEstatisticas() {
        return estatisticaService.getEstatisticas();
    }

    /** GET /api/estatisticas/usuarios-por-periodo — quantidade de usuários por período. */
    @GetMapping("/usuarios-por-periodo")
    public Map<String, Long> usuariosPorPeriodo() {
        return estatisticaService.contarUsuariosPorPeriodo();
    }
}