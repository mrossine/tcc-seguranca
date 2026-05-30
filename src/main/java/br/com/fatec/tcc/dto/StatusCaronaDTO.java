package br.com.fatec.tcc.dto;

/**
 * Payload de notificação de mudança de status da carona.
 * Transmitido via WebSocket para todos os participantes.
 */
public record StatusCaronaDTO(
    Long caronaId,
    String status,
    String mensagem
) {}
