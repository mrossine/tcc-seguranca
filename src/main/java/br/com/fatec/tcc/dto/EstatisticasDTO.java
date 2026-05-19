package br.com.fatec.tcc.dto;

import java.util.Map;

public record EstatisticasDTO(
    Long totalUsuarios,
    Long totalAlertasAtivos,
    Long totalCaronasDisponiveis,
    Map<String, Long> alertasPorTipo,
    Map<Integer, Long> alertasPorHora
) {}