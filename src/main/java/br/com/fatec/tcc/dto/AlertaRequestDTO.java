package br.com.fatec.tcc.dto;

import br.com.fatec.tcc.model.Alerta;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class AlertaRequestDTO {
	private String titulo;
	private String descricao;
	private Alerta.TipoAlerta tipo;
	private String localizacao;
	private Double latitude;
	private Double longitude;

	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private LocalDateTime dataHora;
}
