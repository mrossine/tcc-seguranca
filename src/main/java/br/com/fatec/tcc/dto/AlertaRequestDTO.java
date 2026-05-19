package br.com.fatec.tcc.dto;

import br.com.fatec.tcc.model.Alerta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertaRequestDTO(
	    String titulo,           // agora opcional (pode ser null)
	    String descricao,        // opcional
	    @NotNull(message = "Tipo de ocorrência é obrigatório") Alerta.TipoAlerta tipo,
	    @NotBlank(message = "Localização descritiva é obrigatória") String localizacao,
	    Double latitude,
	    Double longitude
	) {
	
	public static AlertaRequestDTO empty() {
        return new AlertaRequestDTO(null, null, null, null, null, null);
    }
}