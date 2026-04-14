package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.Alerta;
import br.com.fatec.tcc.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findByStatusOrderByDataCriacaoDesc(Alerta.StatusAlerta status);
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
    
    @Query("SELECT HOUR(a.dataHora), COUNT(a) FROM Alerta a GROUP BY HOUR(a.dataHora)")
    List<Object[]> countByHour();
} 
