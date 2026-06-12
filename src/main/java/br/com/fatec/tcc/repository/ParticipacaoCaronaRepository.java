package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.model.ParticipacaoCarona;
import br.com.fatec.tcc.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipacaoCaronaRepository extends JpaRepository<ParticipacaoCarona, Long> {
    List<ParticipacaoCarona> findByPassageiroOrderByDataSolicitacaoDesc(Usuario passageiro);
    List<ParticipacaoCarona> findByCarona(Carona carona);
    Optional<ParticipacaoCarona> findByCaronaAndPassageiro(Carona carona, Usuario passageiro);
    long countByCaronaAndStatus(Carona carona, ParticipacaoCarona.StatusParticipacao status);

    /** Conta participações CONFIRMADAS por carona — batch para evitar N+1 na listagem. */
    @Query("SELECT p.carona.id, COUNT(p) FROM ParticipacaoCarona p " +
           "WHERE p.carona.id IN :ids AND p.status = 'CONFIRMADA' GROUP BY p.carona.id")
    List<Object[]> countConfirmadasByCaronaIds(@Param("ids") Collection<Long> ids);

    /** Participações do usuário logado em um conjunto de caronas — batch para evitar N+1. */
    @Query("SELECT p FROM ParticipacaoCarona p " +
           "WHERE p.carona.id IN :caronaIds AND p.passageiro.id = :userId")
    List<ParticipacaoCarona> findByCaronaIdsAndPassageiroId(
            @Param("caronaIds") Collection<Long> caronaIds, @Param("userId") Long userId);
}
