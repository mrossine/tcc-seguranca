package br.com.fatec.tcc.repository;

import br.com.fatec.tcc.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    //Busca um usuário pelo e-mail (utilizado pelo Spring Security)
    Optional<Usuario> findByEmail(String email);

    //Busca um usuário pela matrícula
    Optional<Usuario> findByMatricula(String matricula);

    //Verifica se já existe um usuário com o e-mail informado
    boolean existsByEmail(String email);
    
    //Verifica se já existe um usuário com a matrícula informada
    boolean existsByMatricula(String matricula);

    //Relatório: quantidade de usuários agrupados por período (MANHA, TARDE, NOITE)     
    @Query("SELECT u.periodo, COUNT(u) FROM Usuario u GROUP BY u.periodo")
    List<Object[]> countByPeriodo();
    	
}