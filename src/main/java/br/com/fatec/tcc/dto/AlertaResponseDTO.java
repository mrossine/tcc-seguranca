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
    LocalDateTime dataCriacao,
    Boolean podeExcluir,

    /** Indica se o alerta foi criado pelo usuário logado (para os filtros "minhas"). */
    Boolean meuAlerta
) {}