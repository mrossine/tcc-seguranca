package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.Alerta;
import br.com.fatec.tcc.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findByStatusOrderByDataCriacaoDesc(Alerta.StatusAlerta status);
    List<Alerta> findByStatusAndDataCriacaoAfterOrderByDataCriacaoDesc(Alerta.StatusAlerta status, LocalDateTime dataLimite);
    List<Alerta> findByUsuarioOrderByDataCriacaoDesc(Usuario usuario);
    
    @Query("SELECT a FROM Alerta a WHERE a.status = 'ATIVO' AND " +
           "(:tipo IS NULL OR a.tipo = :tipo) AND " +
           "(:dataInicio IS NULL OR a.dataHora >= :dataInicio) AND " +
           "(:dataFim IS NULL OR a.dataHora <= :dataFim)")
    List<Alerta> filtrarAlertas(@Param("tipo") Alerta.TipoAlerta tipo,
                                 @Param("dataInicio") LocalDateTime dataInicio,
                                 @Param("dataFim") LocalDateTime dataFim);
    
    @Query("SELECT a.tipo, COUNT(a) FROM Alerta a GROUP BY a.tipo")
    List<Object[]> countByTipo();

    // HOUR() é função MySQL nativa — nativeQuery=true necessário
    @Query(value = "SELECT HOUR(data_hora), COUNT(*) FROM alertas GROUP BY HOUR(data_hora)",
           nativeQuery = true)
    List<Object[]> countByHour();

    /** Incremento atômico do contador de confirmações — evita lost update concorrente. */
    @Modifying
    @Query("UPDATE Alerta a SET a.confirmacoes = a.confirmacoes + 1 WHERE a.id = :id")
    void incrementConfirmacoes(@Param("id") Long id);

    /**
     * Incremento atômico do contador de denúncias e atualização condicional de status.
     * Quando o total atingir 5, o alerta passa para DENUNCIADO na mesma operação.
     */
    /**
     * MySQL avalia as cláusulas SET da esquerda para a direita:
     * após "denuncias = denuncias + 1", a referência a "denuncias" no CASE
     * já é o valor novo (original + 1). Por isso a condição é ">= 5", não "+ 1 >= 5".
     */
    @Modifying
    @Query(value = "UPDATE alertas SET denuncias = denuncias + 1, " +
                   "status = CASE WHEN denuncias >= 5 THEN 'DENUNCIADO' ELSE status END " +
                   "WHERE id = :id",
           nativeQuery = true)
    void incrementDenunciasEAtualizarStatus(@Param("id") Long id);
}
