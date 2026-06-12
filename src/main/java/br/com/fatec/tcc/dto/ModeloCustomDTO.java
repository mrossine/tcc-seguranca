package br.com.fatec.tcc.dto;

import java.time.LocalDateTime;

public record ModeloCustomDTO(
    Long id,
    String solicitanteNome,
    String solicitanteEmail,
    String marca,
    String modelo,
    String status,
    String observacaoAdmin,
    LocalDateTime dataSolicitacao,
    LocalDateTime dataResolucao
) {}
