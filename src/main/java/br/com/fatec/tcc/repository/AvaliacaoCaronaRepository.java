package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.AvaliacaoCarona;
import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AvaliacaoCaronaRepository extends JpaRepository<AvaliacaoCarona, Long> {

    /** Verifica se o passageiro já avaliou esta carona */
    boolean existsByCaronaAndPassageiro(Carona carona, Usuario passageiro);

    /** Lista todas as avaliações recebidas por um motorista */
    List<AvaliacaoCarona> findByMotoristaOrderByDataAvaliacaoDesc(Usuario motorista);

    /** Média de estrelas recebidas pelo motorista */
    @Query("SELECT AVG(a.estrelas) FROM AvaliacaoCarona a WHERE a.motorista = :motorista")
    Double calcularMediaMotorista(@Param("motorista") Usuario motorista);

    /** Total de avaliações recebidas pelo motorista */
    long countByMotorista(Usuario motorista);

    /** Remove todas as avaliações de uma carona (usado ao excluir a carona). */
    void deleteByCarona(Carona carona);

    /** Média e total de avaliações por motorista — batch para evitar N+1 na listagem. */
    @Query("SELECT a.motorista.id, AVG(a.estrelas), COUNT(a) FROM AvaliacaoCarona a " +
           "WHERE a.motorista.id IN :ids GROUP BY a.motorista.id")
    List<Object[]> mediaEContagemByMotoristaIds(@Param("ids") Collection<Long> ids);

    /** IDs de caronas já avaliadas pelo passageiro — batch para evitar N+1 na listagem. */
    @Query("SELECT a.carona.id FROM AvaliacaoCarona a " +
           "WHERE a.carona.id IN :caronaIds AND a.passageiro.id = :userId")
    List<Long> findCaronaIdsAvaliadasPorPassageiro(
            @Param("caronaIds") Collection<Long> caronaIds, @Param("userId") Long userId);
}
