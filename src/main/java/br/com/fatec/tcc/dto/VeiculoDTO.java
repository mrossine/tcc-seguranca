package br.com.fatec.tcc.dto;

import java.time.LocalDateTime;

public record VeiculoDTO(
    Long id,
    String marca,
    String modelo,
    String placa,
    Short ano,
    String cor,
    boolean ativo,
    LocalDateTime dataCadastro
) {}
