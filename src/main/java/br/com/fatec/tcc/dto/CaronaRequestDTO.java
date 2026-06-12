package br.com.fatec.tcc.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class CaronaRequestDTO {
    private String origem;
    private String destino;

    @JsonAlias("dataHoraPartida")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime horarioSaida;
    
    private Integer vagasDisponiveis;
    private String veiculoModelo;
    private String veiculoPlaca;
    private Long veiculoId;
    private String observacoes;

    /** Coordenadas do destino para geofencing (preenchidas via JS no formulário). */
    private Double destinoLatitude;
    private Double destinoLongitude;
} 
