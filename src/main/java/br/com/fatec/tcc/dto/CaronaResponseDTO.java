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
    Carona.StatusCarona status,

    /** Média de avaliações do motorista. Null = ainda em período de avaliação (< 10 caronas). */
    Double mediaAvaliacaoMotorista,

    /** Total de avaliações que o motorista recebeu. */
    Long totalAvaliacoesMotorista,

    /** Indica se o passageiro logado pode avaliar esta carona (FINALIZADA e ainda não avaliou). */
    Boolean podeAvaliar
) {}
