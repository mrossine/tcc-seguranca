package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CaronaRepository extends JpaRepository<Carona, Long> {

	// CORRIGIDO: Adicionar este método
	List<Carona> findByStatusAndHorarioSaidaAfter(Carona.StatusCarona status, LocalDateTime horario);

	List<Carona> findByMotoristaOrderByDataCriacaoDesc(Usuario motorista);

	@Query("SELECT c FROM Carona c WHERE c.status = 'ABERTA' AND " + "c.horarioSaida > :now AND "
			+ "(:origem IS NULL OR c.origem LIKE %:origem%) AND "
			+ "(:destino IS NULL OR c.destino LIKE %:destino%) AND "
			+ "(:horarioInicio IS NULL OR c.horarioSaida >= :horarioInicio) AND "
			+ "(:horarioFim IS NULL OR c.horarioSaida <= :horarioFim)")
	List<Carona> buscarCaronasDisponiveis(@Param("now") LocalDateTime now, @Param("origem") String origem,
			@Param("destino") String destino, @Param("horarioInicio") LocalDateTime horarioInicio,
			@Param("horarioFim") LocalDateTime horarioFim);
}