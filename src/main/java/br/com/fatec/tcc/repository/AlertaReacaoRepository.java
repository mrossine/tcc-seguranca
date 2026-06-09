package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.AlertaReacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertaReacaoRepository extends JpaRepository<AlertaReacao, Long> {

    Optional<AlertaReacao> findByAlertaIdAndUsuarioId(Long alertaId, Long usuarioId);

    long countByAlertaIdAndTipo(Long alertaId, AlertaReacao.TipoReacao tipo);

    /** Remove todas as reações de um alerta (usado ao excluir o alerta). */
    void deleteByAlertaId(Long alertaId);
}
