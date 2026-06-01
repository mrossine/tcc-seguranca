package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.model.DenunciaCarona;
import br.com.fatec.tcc.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DenunciaCaronaRepository extends JpaRepository<DenunciaCarona, Long> {

    /** Evita denúncia duplicada do mesmo denunciante contra o mesmo denunciado na mesma carona. */
    boolean existsByCaronaAndDenuncianteAndDenunciado(Carona carona, Usuario denunciante, Usuario denunciado);

    /** Todas as denúncias, mais recentes primeiro (tela do admin). */
    List<DenunciaCarona> findAllByOrderByDataDenunciaDesc();

    /** Denúncias filtradas por status, mais recentes primeiro. */
    List<DenunciaCarona> findByStatusOrderByDataDenunciaDesc(DenunciaCarona.StatusDenuncia status);

    /** Contagem por status (cards de resumo do admin). */
    long countByStatus(DenunciaCarona.StatusDenuncia status);
}
