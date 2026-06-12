package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.ModeloCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModeloCustomRepository extends JpaRepository<ModeloCustom, Long> {
    List<ModeloCustom> findByStatusOrderByDataSolicitacaoDesc(ModeloCustom.Status status);
    List<ModeloCustom> findAllByOrderByDataSolicitacaoDesc();
    boolean existsByMarcaIgnoreCaseAndModeloIgnoreCaseAndStatus(String marca, String modelo, ModeloCustom.Status status);
}
