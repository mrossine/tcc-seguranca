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

	List<Carona> findByMotoristaOrderByDataCriacaoDesc(Usuario motorista);

	/** Conta total de caronas criadas pelo motorista (para verificar período de avaliação) */
	long countByMotorista(Usuario motorista);

	List<Carona> findByStatusAndHorarioSaidaAfter(Carona.StatusCarona status, LocalDateTime horario);

	@Query("SELECT c FROM Carona c WHERE c.status = 'ABERTA' AND " +
			"c.horarioSaida > :now AND " +
			"(:origem IS NULL OR c.origem LIKE %:origem%) AND " +
			"(:destino IS NULL OR c.destino LIKE %:destino%) AND " +
			"(:horarioInicio IS NULL OR c.horarioSaida >= :horarioInicio) AND " +
			"(:horarioFim IS NULL OR c.horarioSaida <= :horarioFim)")
	List<Carona> buscarCaronasDisponiveis(@Param("now") LocalDateTime now, @Param("origem") String origem,
			@Param("destino") String destino, @Param("horarioInicio") LocalDateTime horarioInicio,
			@Param("horarioFim") LocalDateTime horarioFim);

	// Caronas privadas: CHEIA, FECHADA, COMPLETADA, FINALIZADA e CANCELADA
	// Visíveis apenas para o motorista ou passageiros confirmados
	@Query("SELECT DISTINCT c FROM Carona c LEFT JOIN c.participacoes p " +
			"WHERE c.status IN ('CHEIA', 'FECHADA', 'COMPLETADA', 'FINALIZADA', 'CANCELADA') AND " +
			"(c.motorista.email = :email OR " +
			" (p.passageiro.email = :email AND p.status = 'CONFIRMADA'))")
	List<Carona> buscarCaronasPrivadasDoUsuario(@Param("email") String email);

	// Usado pelo scheduler para fechar caronas cujo horário já passou
	@Query("SELECT c FROM Carona c WHERE c.status IN ('ABERTA', 'CHEIA') AND c.horarioSaida <= :agora")
	List<Carona> findCaronasParaFechar(@Param("agora") LocalDateTime agora);
}