package br.com.fatec.tcc.dto;

import java.time.LocalDateTime;

/**
 * Representação de uma denúncia de alerta para a tela do administrador.
 */
public record DenunciaAlertaAdminDTO(
    Long id,
    Long alertaId,
    String alertaTitulo,
    String alertaTipo,
    String alertaLocalizacao,
    String autorAlertaNome,
    String denuncianteNome,
    String denuncianteEmail,
    String categoria,
    String justificativa,
    String status,
    LocalDateTime dataDenuncia
) {}
