package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	Optional<Usuario> findByEmail(String email);

	Optional<Usuario> findByMatricula(String matricula);

	boolean existsByEmail(String email);

	boolean existsByMatricula(String matricula);
}