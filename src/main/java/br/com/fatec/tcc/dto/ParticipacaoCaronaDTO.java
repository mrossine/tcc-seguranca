package br.com.fatec.tcc.dto;

import br.com.fatec.tcc.model.ParticipacaoCarona;

public record ParticipacaoCaronaDTO(
    Long id,
    String passageiroNome,
    String passageiroEmail,
    ParticipacaoCarona.StatusParticipacao status
) {}