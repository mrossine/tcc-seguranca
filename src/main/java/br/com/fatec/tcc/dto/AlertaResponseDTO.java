package br.com.fatec.tcc.dto;

import br.com.fatec.tcc.model.Alerta;
import java.time.LocalDateTime;

public record AlertaResponseDTO(
    Long id,
    String titulo,
    String descricao,
    Alerta.TipoAlerta tipo,
    String localizacao,
    Double latitude,
    Double longitude,
    LocalDateTime dataHora,
    Alerta.StatusAlerta status,
    String nomeUsuario,
    Integer confirmacoes,
    Integer denuncias,
    LocalDateTime dataCriacao
) {}