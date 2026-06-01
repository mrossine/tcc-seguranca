package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.AlertaRequestDTO;
import br.com.fatec.tcc.dto.AlertaResponseDTO;
import br.com.fatec.tcc.model.Alerta;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Regras de negócio dos ALERTAS de segurança.
 *
 * Cuida de criar, listar, confirmar, denunciar e remover alertas, além de
 * preencher informações derivadas (ex.: se o alerta é do usuário logado e se
 * ele pode excluí-lo).
 */
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final UsuarioService usuarioService;

    /**
     * Cria um novo alerta (INSERÇÃO no banco).
     * Associa o alerta ao usuário logado, marca como ATIVO e grava.
     */
    @Transactional
    public AlertaResponseDTO criarAlerta(AlertaRequestDTO request, String email) {
        Usuario usuario = usuarioService.findUserByUsername(email);

        Alerta alerta = new Alerta();
        alerta.setTitulo(request.titulo());
        alerta.setDescricao(request.descricao());
        alerta.setTipo(request.tipo());
        alerta.setLocalizacao(request.localizacao());
        alerta.setLatitude(request.latitude());
        alerta.setLongitude(request.longitude());
        alerta.setDataHora(LocalDateTime.now());
        alerta.setUsuario(usuario);
        alerta.setStatus(Alerta.StatusAlerta.ATIVO);

        Alerta saved = alertaRepository.save(alerta);
        return convertToResponseDTO(saved, usuario);
    }

    /**
     * Lista alertas ativos dos últimos 14 dias (visibilidade pública).
     * Alertas mais antigos permanecem no banco para estatísticas.
     */
    public List<AlertaResponseDTO> listarAlertasAtivos(String emailLogado) {
        Usuario usuarioLogado = usuarioService.findUserByUsername(emailLogado);
        LocalDateTime limite = LocalDateTime.now().minusDays(14);
        List<Alerta> alertas = alertaRepository
                .findByStatusAndDataCriacaoAfterOrderByDataCriacaoDesc(Alerta.StatusAlerta.ATIVO, limite);
        return alertas.stream()
                .map(alerta -> convertToResponseDTO(alerta, usuarioLogado))
                .collect(Collectors.toList());
    }

    /**
     * Confirma um alerta como verdadeiro (ALTERAÇÃO).
     * Incrementa o contador de confirmações e salva.
     */
    @Transactional
    public void confirmarAlerta(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
        alerta.setConfirmacoes(alerta.getConfirmacoes() + 1);
        alertaRepository.save(alerta);
    }

    /**
     * Denuncia um alerta como falso/inadequado (ALTERAÇÃO).
     * Incrementa o contador de denúncias e, ao atingir 5, marca como DENUNCIADO
     * (o alerta deixa de aparecer na listagem pública).
     */
    @Transactional
    public void denunciarAlerta(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
        alerta.setDenuncias(alerta.getDenuncias() + 1);
        if (alerta.getDenuncias() >= 5) {
            alerta.setStatus(Alerta.StatusAlerta.DENUNCIADO);
        }
        alertaRepository.save(alerta);
    }

    /**
     * Remove um alerta (EXCLUSÃO).
     * Só o autor do alerta, um moderador ou um admin têm permissão.
     */
    @Transactional
    public void removerAlerta(Long id, String email) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
        Usuario usuario = usuarioService.findUserByUsername(email);

        // Permissão: autor do alerta, moderador ou admin
        if (!alerta.getUsuario().getId().equals(usuario.getId()) &&
            usuario.getRole() != Usuario.Role.MODERATOR &&
            usuario.getRole() != Usuario.Role.ADMIN) {
            throw new RuntimeException("Sem permissão para remover este alerta");
        }
        alertaRepository.delete(alerta);
    }

    /** Lista (CONSULTA) todos os alertas criados por um usuário específico. */
    public List<AlertaResponseDTO> listarAlertasPorUsuario(Usuario usuario) {
        return alertaRepository.findByUsuarioOrderByDataCriacaoDesc(usuario)
                .stream()
                .map(alerta -> convertToResponseDTO(alerta, usuario))
                .collect(Collectors.toList());
    }

    /**
     * Converte a entidade Alerta no DTO enviado ao frontend, calculando dois campos
     * derivados a partir do usuário logado:
     *  - meuAlerta : se o alerta foi criado por ele (usado nos filtros "meus alertas");
     *  - podeExcluir : se ele tem permissão para excluir (autor, moderador ou admin).
     */
    private AlertaResponseDTO convertToResponseDTO(Alerta alerta, Usuario usuarioLogado) {
        boolean podeExcluir = false;
        boolean meuAlerta = false;
        if (usuarioLogado != null) {
            meuAlerta = alerta.getUsuario().getId().equals(usuarioLogado.getId());
            podeExcluir = meuAlerta ||
                          usuarioLogado.getRole() == Usuario.Role.ADMIN ||
                          usuarioLogado.getRole() == Usuario.Role.MODERATOR;
        }
        return new AlertaResponseDTO(
            alerta.getId(),
            alerta.getTitulo(),
            alerta.getDescricao(),
            alerta.getTipo(),
            alerta.getLocalizacao(),
            alerta.getLatitude(),
            alerta.getLongitude(),
            alerta.getDataHora(),
            alerta.getStatus(),
            alerta.getUsuario().getNomeCompleto(),
            alerta.getConfirmacoes(),
            alerta.getDenuncias(),
            alerta.getDataCriacao(),
            podeExcluir,
            meuAlerta
        );
    }
}