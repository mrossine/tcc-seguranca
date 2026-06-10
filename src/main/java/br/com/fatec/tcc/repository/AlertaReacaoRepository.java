package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.AlertaReacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertaReacaoRepository extends JpaRepository<AlertaReacao, Long> {

    Optional<AlertaReacao> findByAlertaIdAndUsuarioId(Long alertaId, Long usuarioId);

    long countByAlertaIdAndTipo(Long alertaId, AlertaReacao.TipoReacao tipo);

    /** Remove todas as reações de um alerta (usado ao excluir o alerta). */
    void deleteByAlertaId(Long alertaId);

    /**
     * Retorna contagens de reações agrupadas por alerta e tipo para uma lista de IDs.
     * Cada Object[] tem: [alertaId (Long), tipo (TipoReacao), count (Long)].
     * Substitui N queries individuais por uma única query com GROUP BY.
     */
    @Query("SELECT ar.alerta.id, ar.tipo, COUNT(ar) FROM AlertaReacao ar " +
           "WHERE ar.alerta.id IN :alertaIds GROUP BY ar.alerta.id, ar.tipo")
    List<Object[]> countsByAlertaIds(@Param("alertaIds") Collection<Long> alertaIds);
}
