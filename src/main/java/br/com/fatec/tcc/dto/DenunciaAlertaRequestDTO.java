package br.com.fatec.tcc.dto;

/**
 * Payload para registrar a denúncia de um alerta.
 *
 * - categoria      : nome do enum CategoriaDenuncia (ex.: "CONTEUDO_FALSO")
 * - justificativa  : texto livre explicando o motivo da denúncia
 */
public record DenunciaAlertaRequestDTO(
    String categoria,
    String justificativa
) {}
