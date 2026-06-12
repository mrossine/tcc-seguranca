package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    List<Veiculo> findByUsuarioIdAndAtivoTrueOrderByDataCadastroDesc(Long usuarioId);
    boolean existsByPlaca(String placa);
    Optional<Veiculo> findByPlaca(String placa);
}
