package br.com.fatec.tcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VeiculoRequestDTO(
    @NotBlank(message = "Marca é obrigatória")
    @Size(max = 100)
    String marca,

    @NotBlank(message = "Modelo é obrigatório")
    @Size(max = 150)
    String modelo,

    @NotBlank(message = "Placa é obrigatória")
    @Pattern(regexp = "^[A-Z]{3}[0-9]{1}[A-Z0-9]{1}[0-9]{2}$|^[A-Z]{3}[0-9]{4}$",
             message = "Placa inválida. Use o formato ABC1234 (antigo) ou ABC1D23 (Mercosul)")
    String placa,

    Short ano,

    @Size(max = 50)
    String cor
) {}
