package br.com.fatec.tcc.dto;

import lombok.Data;

import java.util.Map;

@Data
public class EstatisticasDTO {
    private Long totalUsuarios;
    private Long totalAlertasAtivos;
    private Long totalCaronasDisponiveis;
    private Map<String, Long> alertasPorTipo;
    private Map<Integer, Long> alertasPorHora;
} 
