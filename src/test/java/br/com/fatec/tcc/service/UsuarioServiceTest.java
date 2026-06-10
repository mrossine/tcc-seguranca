package br.com.fatec.tcc.service;

import br.com.fatec.tcc.model.Usuario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioServiceTest {

    // ─── alunoExpirado ────────────────────────────────────────────────────────

    @Test
    void alunoAtivoDentroDosPrazosNaoEhExpirado() {
        Usuario u = aluno("202300001");
        assertThat(UsuarioService.alunoExpirado(u)).isFalse();
    }

    @Test
    void alunoComIngresso2015EhExpirado() {
        Usuario u = aluno("201500001");
        assertThat(UsuarioService.alunoExpirado(u)).isTrue();
    }

    @Test
    void docenteNuncaEhExpirado() {
        Usuario u = aluno("201500001");
        u.setTipoUsuario(Usuario.TipoUsuario.DOCENTE);
        assertThat(UsuarioService.alunoExpirado(u)).isFalse();
    }

    @Test
    void adminComMatriculaAntigaNaoEhExpirado() {
        Usuario u = aluno("201500001");
        u.setRole(Usuario.Role.ADMIN);
        assertThat(UsuarioService.alunoExpirado(u)).isFalse();
    }

    @Test
    void moderadorComMatriculaAntigaNaoEhExpirado() {
        Usuario u = aluno("201500001");
        u.setRole(Usuario.Role.MODERATOR);
        assertThat(UsuarioService.alunoExpirado(u)).isFalse();
    }

    @Test
    void matriculaSemAnoNaoEhExpirada() {
        Usuario u = aluno("ADMIN001");
        assertThat(UsuarioService.alunoExpirado(u)).isFalse();
    }

    // ─── extrairAnoIngresso ────────────────────────────────────────────────────

    @Test
    void extraiAnoNoInicioDeMatricula() {
        assertThat(UsuarioService.extrairAnoIngresso("202300001")).isEqualTo(2023);
    }

    @Test
    void extraiAnoNoMeioDeMatricula() {
        assertThat(UsuarioService.extrairAnoIngresso("A2021001")).isEqualTo(2021);
    }

    @Test
    void retornaNull_quandoNaoHaAno() {
        assertThat(UsuarioService.extrairAnoIngresso("ADMIN001")).isNull();
    }

    @Test
    void retornaNull_paraNulo() {
        assertThat(UsuarioService.extrairAnoIngresso(null)).isNull();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Usuario aluno(String matricula) {
        Usuario u = new Usuario();
        u.setTipoUsuario(Usuario.TipoUsuario.ALUNO);
        u.setRole(Usuario.Role.USER);
        u.setMatricula(matricula);
        return u;
    }
}
