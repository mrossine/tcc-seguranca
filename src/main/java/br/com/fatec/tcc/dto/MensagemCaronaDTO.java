package br.com.fatec.tcc.dto;

import br.com.fatec.tcc.model.MensagemCarona;

import java.time.LocalDateTime;

/**
 * DTO de uma mensagem do chat da carona.
 * Inclui identificação do remetente para que o frontend
 * possa diferenciar mensagens próprias das alheias.
 */
public record MensagemCaronaDTO(
    Long id,
    Long caronaId,
    String remetenteNome,
    String remetenteEmail,
    String conteudo,
    LocalDateTime dataCriacao,
    Boolean lido
) {
    public static MensagemCaronaDTO fromEntity(MensagemCarona m) {
        return new MensagemCaronaDTO(
            m.getId(),
            m.getCarona().getId(),
            m.getRemetente().getNomeCompleto(),
            m.getRemetente().getEmail(),
            m.getConteudo(),
            m.getDataCriacao(),
            m.getLido()
        );
    }
}
