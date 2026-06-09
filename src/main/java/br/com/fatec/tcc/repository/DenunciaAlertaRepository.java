package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.Alerta;
import br.com.fatec.tcc.model.DenunciaAlerta;
import br.com.fatec.tcc.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DenunciaAlertaRepository extends JpaRepository<DenunciaAlerta, Long> {

    /** Evita que o mesmo usuário denuncie o mesmo alerta mais de uma vez. */
    boolean existsByAlertaAndDenunciante(Alerta alerta, Usuario denunciante);

    /** Todas as denúncias de alerta, mais recentes primeiro (tela do admin). */
    List<DenunciaAlerta> findAllByOrderByDataDenunciaDesc();

    /** Denúncias de alerta filtradas por status, mais recentes primeiro. */
    List<DenunciaAlerta> findByStatusOrderByDataDenunciaDesc(DenunciaAlerta.StatusDenuncia status);

    /** Remove todas as denúncias de um alerta (usado ao excluir o alerta). */
    void deleteByAlerta(Alerta alerta);
}
