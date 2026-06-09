package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.MensagemCarona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemCaronaRepository extends JpaRepository<MensagemCarona, Long> {

    List<MensagemCarona> findByCaronaIdOrderByDataCriacaoAsc(Long caronaId);

    List<MensagemCarona> findByCaronaIdAndLidoFalseAndRemetenteIdNot(Long caronaId, Long usuarioId);

    long countByCaronaIdAndLidoFalseAndRemetenteIdNot(Long caronaId, Long usuarioId);

    /** Remove todas as mensagens de uma carona (usado ao excluir a carona). */
    void deleteByCaronaId(Long caronaId);
}
