package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.AlertaConfirmacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AlertaConfirmacaoRepository extends JpaRepository<AlertaConfirmacao, Long> {
    boolean existsByAlertaIdAndUsuarioId(Long alertaId, Long usuarioId);

    long countByAlertaId(Long alertaId);

    @Query("SELECT ac.alerta.id, COUNT(ac) FROM AlertaConfirmacao ac " +
           "WHERE ac.alerta.id IN :ids GROUP BY ac.alerta.id")
    List<Object[]> countsByAlertaIds(@Param("ids") Collection<Long> ids);

    void deleteByAlertaId(Long alertaId);
}
