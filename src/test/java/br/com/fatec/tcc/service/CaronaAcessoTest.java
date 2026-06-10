package br.com.fatec.tcc.service;

import br.com.fatec.tcc.model.*;
import br.com.fatec.tcc.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Testa as regras de controle de acesso (IDOR fix) no buscarPorId.
 */
@ExtendWith(MockitoExtension.class)
class CaronaAcessoTest {

    @Mock CaronaRepository caronaRepository;
    @Mock ParticipacaoCaronaRepository participacaoRepository;
    @Mock AvaliacaoCaronaRepository avaliacaoRepository;
    @Mock DenunciaCaronaRepository denunciaRepository;
    @Mock MensagemCaronaRepository mensagemRepository;
    @Mock UsuarioService usuarioService;
    @Mock MensagemCaronaService mensagemCaronaService;

    @InjectMocks CaronaService caronaService;

    private Usuario motorista;
    private Usuario passageiroConfirmado;
    private Usuario estranho;
    private Usuario admin;
    private Carona caronaCheia;

    @BeforeEach
    void setUp() {
        motorista = usuario(1L, Usuario.Role.USER);
        passageiroConfirmado = usuario(2L, Usuario.Role.USER);
        estranho = usuario(3L, Usuario.Role.USER);
        admin = usuario(4L, Usuario.Role.ADMIN);

        caronaCheia = new Carona();
        caronaCheia.setId(100L);
        caronaCheia.setMotorista(motorista);
        caronaCheia.setStatus(Carona.StatusCarona.CHEIA);
        caronaCheia.setVagasDisponiveis(1);
        caronaCheia.setOrigem("A");
        caronaCheia.setDestino("B");
    }

    @Test
    void motoristaPodeVerCaronaCheia() {
        when(caronaRepository.findById(100L)).thenReturn(Optional.of(caronaCheia));
        when(usuarioService.findUserByUsername("motorista@fatec.sp.gov.br")).thenReturn(motorista);
        stubCounts();

        assertThatCode(() -> caronaService.buscarPorId(100L, "motorista@fatec.sp.gov.br"))
                .doesNotThrowAnyException();
    }

    @Test
    void passageiroConfirmadoPoDeVerCaronaCheia() {
        ParticipacaoCarona p = new ParticipacaoCarona();
        p.setStatus(ParticipacaoCarona.StatusParticipacao.CONFIRMADA);

        when(caronaRepository.findById(100L)).thenReturn(Optional.of(caronaCheia));
        when(usuarioService.findUserByUsername("passageiro@fatec.sp.gov.br")).thenReturn(passageiroConfirmado);
        when(participacaoRepository.findByCaronaAndPassageiro(caronaCheia, passageiroConfirmado))
                .thenReturn(Optional.of(p));
        stubCounts();

        assertThatCode(() -> caronaService.buscarPorId(100L, "passageiro@fatec.sp.gov.br"))
                .doesNotThrowAnyException();
    }

    @Test
    void estranhoNaoPodeVerCaronaCheia() {
        when(caronaRepository.findById(100L)).thenReturn(Optional.of(caronaCheia));
        when(usuarioService.findUserByUsername("estranho@fatec.sp.gov.br")).thenReturn(estranho);
        when(participacaoRepository.findByCaronaAndPassageiro(caronaCheia, estranho))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> caronaService.buscarPorId(100L, "estranho@fatec.sp.gov.br"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Acesso negado");
    }

    @Test
    void adminSemprePoDeVerQualquerCarona() {
        when(caronaRepository.findById(100L)).thenReturn(Optional.of(caronaCheia));
        when(usuarioService.findUserByUsername("admin@fatec.sp.gov.br")).thenReturn(admin);
        stubCounts();

        assertThatCode(() -> caronaService.buscarPorId(100L, "admin@fatec.sp.gov.br"))
                .doesNotThrowAnyException();
    }

    @Test
    void qualquerUsuarioPoDeVerCaronaAberta() {
        caronaCheia.setStatus(Carona.StatusCarona.ABERTA);
        when(caronaRepository.findById(100L)).thenReturn(Optional.of(caronaCheia));
        when(usuarioService.findUserByUsername("estranho@fatec.sp.gov.br")).thenReturn(estranho);
        stubCounts();

        assertThatCode(() -> caronaService.buscarPorId(100L, "estranho@fatec.sp.gov.br"))
                .doesNotThrowAnyException();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private void stubCounts() {
        when(participacaoRepository.countByCaronaAndStatus(any(), any())).thenReturn(0L);
        when(caronaRepository.countByMotorista(any())).thenReturn(0L);
        when(avaliacaoRepository.countByMotorista(any())).thenReturn(0L);
    }

    private Usuario usuario(Long id, Usuario.Role role) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRole(role);
        u.setNomeCompleto("Usuário " + id);
        u.setEmail("usuario" + id + "@fatec.sp.gov.br");
        return u;
    }
}
