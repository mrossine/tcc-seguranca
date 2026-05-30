package br.com.fatec.tcc.dto;

/**
 * Payload enviado pelo motorista via WebSocket com sua localização atual.
 * Também reutilizado para broadcast aos passageiros.
 */
public record LocalizacaoDTO(
    Long caronaId,
    double latitude,
    double longitude
) {}
