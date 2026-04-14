package br.com.fatec.tcc.dto;

import br.com.fatec.tcc.model.Carona;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaronaResponseDTO {
    private Long id;
    private String motoristaNome;
    private String origem;
    private String destino;
    private LocalDateTime horarioSaida;
    private Integer vagasDisponiveis;
    private Integer vagasOcupadas;
    private String veiculoModelo;
    private String veiculoPlaca;
    private String observacoes;
    private Carona.StatusCarona status;
} 
