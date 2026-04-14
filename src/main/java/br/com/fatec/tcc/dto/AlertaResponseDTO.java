package br.com.fatec.tcc.dto;

import br.com.fatec.tcc.model.Alerta;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertaResponseDTO {
	private Long id;
	private String titulo;
	private String descricao;
	private Alerta.TipoAlerta tipo;
	private String localizacao;
	private Double latitude;
	private Double longitude;
	private LocalDateTime dataHora;
	private Alerta.StatusAlerta status;
	private String nomeUsuario;
	private Integer confirmacoes;
	private Integer denuncias;
	private LocalDateTime dataCriacao;
}
