package br.com.fatec.tcc.dto;

import java.time.LocalDateTime;

/**
 * Representação de uma denúncia para a tela do administrador.
 */
public record DenunciaAdminDTO(
    Long id,
    Long caronaId,
    String origem,
    String destino,
    LocalDateTime horarioSaida,
    String tipoDenunciante,
    String denuncianteNome,
    String denuncianteEmail,
    String denunciadoNome,
    String denunciadoEmail,
    String categoria,
    String descricao,
    String status,
    LocalDateTime dataDenuncia
) {}
