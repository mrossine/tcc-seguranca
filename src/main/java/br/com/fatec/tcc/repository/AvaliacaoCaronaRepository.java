package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.AvaliacaoCarona;
import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
