package br.com.fatec.tcc.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class CaronaRequestDTO {
    private String origem;
    private String destino;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime horarioSaida;
    
    private Integer vagasDisponiveis;
    private String veiculoModelo;
    private String veiculoPlaca;
    private String observacoes;
} 
