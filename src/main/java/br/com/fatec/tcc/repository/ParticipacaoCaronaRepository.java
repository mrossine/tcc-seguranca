package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.model.ParticipacaoCarona;
import br.com.fatec.tcc.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipacaoCaronaRepository extends JpaRepository<ParticipacaoCarona, Long> {
    List<ParticipacaoCarona> findByPassageiroOrderByDataSolicitacaoDesc(Usuario passageiro);
    List<ParticipacaoCarona> findByCarona(Carona carona);
    Optional<ParticipacaoCarona> findByCaronaAndPassageiro(Carona carona, Usuario passageiro);
    long countByCaronaAndStatus(Carona carona, ParticipacaoCarona.StatusParticipacao status);
} 
