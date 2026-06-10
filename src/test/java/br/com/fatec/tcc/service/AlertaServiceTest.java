package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.AlertaResponseDTO;
import br.com.fatec.tcc.model.*;
import br.com.fatec.tcc.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock AlertaRepository alertaRepository;
    @Mock AlertaReacaoRepository reacaoRepository;
    @Mock AlertaConfirmacaoRepository confirmacaoRepository;
    @Mock DenunciaAlertaRepository denunciaAlertaRepository;
    @Mock UsuarioService usuarioService;

    @InjectMocks AlertaService alertaService;

    private Usuario autor;
    private Usuario outroUsuario;
    private Alerta alerta;

    @BeforeEach
    void setUp() {
        autor = usuario(1L, Usuario.Role.USER);
        outroUsuario = usuario(2L, Usuario.Role.USER);

        alerta = new Alerta();
        alerta.setId(10L);
        alerta.setUsuario(autor);
        alerta.setStatus(Alerta.StatusAlerta.ATIVO);
        alerta.setConfirmacoes(0);
        alerta.setDenuncias(0);
        alerta.setDataHora(LocalDateTime.now());
    }

    // ─── removerAlerta ────────────────────────────────────────────────────────

    @Test
    void autorPodeRemoverSeuProprioAlerta() {
        when(alertaRepository.findById(10L)).thenReturn(Optional.of(alerta));
        when(usuarioService.findUserByUsername("autor@fatec.sp.gov.br")).thenReturn(autor);

        alertaService.removerAlerta(10L, "autor@fatec.sp.gov.br");

        verify(alertaRepository).delete(alerta);
    }

    @Test
    void adminPodeRemoverQualquerAlerta() {
        Usuario admin = usuario(3L, Usuario.Role.ADMIN);
        when(alertaRepository.findById(10L)).thenReturn(Optional.of(alerta));
        when(usuarioService.findUserByUsername("admin@fatec.sp.gov.br")).thenReturn(admin);

        alertaService.removerAlerta(10L, "admin@fatec.sp.gov.br");

        verify(alertaRepository).delete(alerta);
    }

    @Test
    void usuarioComumNaoPodeRemoverAlertaAlheio() {
        when(alertaRepository.findById(10L)).thenReturn(Optional.of(alerta));
        when(usuarioService.findUserByUsername("outro@fatec.sp.gov.br")).thenReturn(outroUsuario);

        assertThatThrownBy(() -> alertaService.removerAlerta(10L, "outro@fatec.sp.gov.br"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("permissão");

        verify(alertaRepository, never()).delete(any());
    }

    // ─── confirmarAlerta ──────────────────────────────────────────────────────

    @Test
    void usuarioNaoPodeConfirmarMesmoAlertaDuasVezes() {
        when(alertaRepository.findById(10L)).thenReturn(Optional.of(alerta));
        when(usuarioService.findUserByUsername("outro@fatec.sp.gov.br")).thenReturn(outroUsuario);
        when(confirmacaoRepository.existsByAlertaIdAndUsuarioId(10L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> alertaService.confirmarAlerta(10L, "outro@fatec.sp.gov.br"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("já confirmou");
    }

    // ─── denunciarAlerta ──────────────────────────────────────────────────────

    @Test
    void usuarioNaoPodeDenunciarMesmoAlertaDuasVezes() {
        when(alertaRepository.findById(10L)).thenReturn(Optional.of(alerta));
        when(usuarioService.findUserByUsername("outro@fatec.sp.gov.br")).thenReturn(outroUsuario);
        when(denunciaAlertaRepository.existsByAlertaAndDenunciante(alerta, outroUsuario)).thenReturn(true);

        assertThatThrownBy(() -> alertaService.denunciarAlerta(
                10L, "outro@fatec.sp.gov.br", "SPAM", "justificativa válida"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("já denunciou");
    }

    @Test
    void alertaMarcadoComoDenunciadoAoAtingir5Denuncias() {
        alerta.setDenuncias(4);
        when(alertaRepository.findById(10L)).thenReturn(Optional.of(alerta));
        when(usuarioService.findUserByUsername("outro@fatec.sp.gov.br")).thenReturn(outroUsuario);
        when(denunciaAlertaRepository.existsByAlertaAndDenunciante(alerta, outroUsuario)).thenReturn(false);

        alertaService.denunciarAlerta(10L, "outro@fatec.sp.gov.br", "SPAM", "justificativa válida");

        assertThat(alerta.getStatus()).isEqualTo(Alerta.StatusAlerta.DENUNCIADO);
    }

    @Test
    void justificativaCurtaDemaisEhRejeitada() {
        when(alertaRepository.findById(10L)).thenReturn(Optional.of(alerta));
        when(usuarioService.findUserByUsername("outro@fatec.sp.gov.br")).thenReturn(outroUsuario);

        // validarJustificativa lança exceção antes do check de duplicidade
        assertThatThrownBy(() -> alertaService.denunciarAlerta(
                10L, "outro@fatec.sp.gov.br", "SPAM", "ok"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("5 caracteres");
    }

    // ─── listarAlertasAtivos — sem N+1 ────────────────────────────────────────

    @Test
    void listarAlertasUsaBatchLoadDeReacoes() {
        when(usuarioService.findUserByUsername("autor@fatec.sp.gov.br")).thenReturn(autor);
        when(alertaRepository.findByStatusAndDataCriacaoAfterOrderByDataCriacaoDesc(
                eq(Alerta.StatusAlerta.ATIVO), any())).thenReturn(List.of(alerta));
        when(reacaoRepository.countsByAlertaIds(anyCollection())).thenReturn(List.of());

        List<AlertaResponseDTO> resultado = alertaService.listarAlertasAtivos("autor@fatec.sp.gov.br");

        assertThat(resultado).hasSize(1);
        // Verifica que countByAlertaIdAndTipo (query individual) NUNCA foi chamado
        verify(reacaoRepository, never()).countByAlertaIdAndTipo(anyLong(), any());
        verify(reacaoRepository, times(1)).countsByAlertaIds(anyCollection());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Usuario usuario(Long id, Usuario.Role role) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRole(role);
        u.setNomeCompleto("Usuário " + id);
        u.setEmail("usuario" + id + "@fatec.sp.gov.br");
        return u;
    }
}
