package br.com.fatec.tcc.dto;

import br.com.fatec.tcc.model.Carona;
import java.time.LocalDateTime;

public record CaronaResponseDTO(
    Long id,
    String motoristaNome,
    String motoristaEmail,
    String origem,
    String destino,
    LocalDateTime horarioSaida,
    Integer vagasDisponiveis,
    Integer vagasOcupadas,
    String veiculoModelo,
    String veiculoPlaca,
    String observacoes,
    Carona.StatusCarona status
) {}