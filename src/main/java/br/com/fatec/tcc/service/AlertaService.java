package br.com.fatec.tcc.service;

import br.com.fatec.tcc.dto.AlertaRequestDTO;
import br.com.fatec.tcc.dto.AlertaResponseDTO;
import br.com.fatec.tcc.dto.DenunciaAlertaAdminDTO;
import br.com.fatec.tcc.model.Alerta;
import br.com.fatec.tcc.model.AlertaReacao;
import br.com.fatec.tcc.model.DenunciaAlerta;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.AlertaReacaoRepository;
import br.com.fatec.tcc.repository.AlertaRepository;
import br.com.fatec.tcc.repository.DenunciaAlertaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Regras de negócio dos ALERTAS de segurança.
 *
 * Cuida de criar, listar, confirmar, denunciar e remover alertas, além de
 * preencher informações derivadas (ex.: se o alerta é do usuário logado e se
 * ele pode excluí-lo).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final AlertaReacaoRepository reacaoRepository;
    private final DenunciaAlertaRepository denunciaAlertaRepository;
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
     * Denuncia um alerta como falso/inadequado, registrando categoria e justificativa.
     *
     * Cria uma DenunciaAlerta (visível ao admin), incrementa o contador de denúncias e,
     * ao atingir 5, marca o alerta como DENUNCIADO (deixa de aparecer na listagem pública).
     * Um usuário só pode denunciar o mesmo alerta uma vez.
     */
    @Transactional
    public void denunciarAlerta(Long id, String email, String categoriaStr, String justificativa) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
        Usuario usuario = usuarioService.findUserByUsername(email);

        DenunciaAlerta.CategoriaDenuncia categoria = parseCategoria(categoriaStr);
        validarJustificativa(justificativa);

        if (denunciaAlertaRepository.existsByAlertaAndDenunciante(alerta, usuario)) {
            throw new RuntimeException("Você já denunciou este alerta.");
        }

        DenunciaAlerta denuncia = new DenunciaAlerta();
        denuncia.setAlerta(alerta);
        denuncia.setDenunciante(usuario);
        denuncia.setCategoria(categoria);
        denuncia.setJustificativa(justificativa.trim());
        denuncia.setStatus(DenunciaAlerta.StatusDenuncia.PENDENTE);
        denunciaAlertaRepository.save(denuncia);

        alerta.setDenuncias(alerta.getDenuncias() + 1);
        if (alerta.getDenuncias() >= 5) {
            alerta.setStatus(Alerta.StatusAlerta.DENUNCIADO);
        }
        alertaRepository.save(alerta);
        log.info("Alerta {} denunciado por {} — categoria={}", id, email, categoria);
    }

    private DenunciaAlerta.CategoriaDenuncia parseCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            throw new RuntimeException("Selecione o motivo da denúncia.");
        }
        try {
            return DenunciaAlerta.CategoriaDenuncia.valueOf(categoria.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Motivo de denúncia inválido.");
        }
    }

    private void validarJustificativa(String justificativa) {
        if (justificativa == null || justificativa.trim().length() < 5) {
            throw new RuntimeException("Descreva o motivo da denúncia com pelo menos 5 caracteres.");
        }
        if (justificativa.trim().length() > 1000) {
            throw new RuntimeException("A justificativa não pode passar de 1000 caracteres.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Denúncias de alerta — administração
    // ─────────────────────────────────────────────────────────────────────────

    /** Lista todas as denúncias de alerta (opcionalmente filtradas por status) para o admin. */
    public List<DenunciaAlertaAdminDTO> listarDenunciasAlerta(String statusFiltro) {
        List<DenunciaAlerta> lista;
        if (statusFiltro == null || statusFiltro.isBlank()) {
            lista = denunciaAlertaRepository.findAllByOrderByDataDenunciaDesc();
        } else {
            DenunciaAlerta.StatusDenuncia status;
            try {
                status = DenunciaAlerta.StatusDenuncia.valueOf(statusFiltro.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Status inválido.");
            }
            lista = denunciaAlertaRepository.findByStatusOrderByDataDenunciaDesc(status);
        }
        return lista.stream().map(this::toDenunciaAlertaAdminDTO).collect(Collectors.toList());
    }

    /** Atualiza o status de uma denúncia de alerta (PENDENTE, EM_ANALISE, RESOLVIDA, ARQUIVADA). */
    @Transactional
    public void atualizarStatusDenunciaAlerta(Long denunciaId, String novoStatus) {
        DenunciaAlerta d = denunciaAlertaRepository.findById(denunciaId)
                .orElseThrow(() -> new RuntimeException("Denúncia não encontrada"));
        if (novoStatus == null || novoStatus.isBlank()) {
            throw new RuntimeException("Informe o novo status.");
        }
        try {
            d.setStatus(DenunciaAlerta.StatusDenuncia.valueOf(novoStatus.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Status inválido.");
        }
        denunciaAlertaRepository.save(d);
    }

    private DenunciaAlertaAdminDTO toDenunciaAlertaAdminDTO(DenunciaAlerta d) {
        Alerta a = d.getAlerta();
        return new DenunciaAlertaAdminDTO(
                d.getId(),
                a.getId(),
                a.getTitulo(),
                a.getTipo() != null ? a.getTipo().name() : null,
                a.getLocalizacao(),
                a.getUsuario().getNomeCompleto(),
                d.getDenunciante().getNomeCompleto(),
                d.getDenunciante().getEmail(),
                d.getCategoria().name(),
                d.getJustificativa(),
                d.getStatus().name(),
                d.getDataDenuncia()
        );
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
        // Remove os dependentes antes (reações e denúncias não têm cascade)
        reacaoRepository.deleteByAlertaId(id);
        denunciaAlertaRepository.deleteByAlerta(alerta);
        alertaRepository.delete(alerta);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reações (LIKE / DISLIKE)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Alterna a reação do usuário no alerta (toggle).
     * - Mesmo tipo já existente → remove a reação.
     * - Tipo diferente → troca para o novo.
     * - Sem reação prévia → cria nova.
     */
    @Transactional
    public Map<String, Long> reagirAlerta(Long alertaId, AlertaReacao.TipoReacao tipo, String email) {
        Alerta alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
        Usuario usuario = usuarioService.findUserByUsername(email);

        Optional<AlertaReacao> existente = reacaoRepository.findByAlertaIdAndUsuarioId(alertaId, usuario.getId());

        if (existente.isPresent()) {
            AlertaReacao reacao = existente.get();
            if (reacao.getTipo() == tipo) {
                reacaoRepository.delete(reacao);
                log.info("Reação removida — alerta={} usuario={} tipo={}", alertaId, email, tipo);
            } else {
                reacao.setTipo(tipo);
                reacaoRepository.save(reacao);
                log.info("Reação alterada — alerta={} usuario={} tipo={}", alertaId, email, tipo);
            }
        } else {
            AlertaReacao nova = new AlertaReacao();
            nova.setAlerta(alerta);
            nova.setUsuario(usuario);
            nova.setTipo(tipo);
            reacaoRepository.save(nova);
            log.info("Reação criada — alerta={} usuario={} tipo={}", alertaId, email, tipo);
        }

        return obterContadoresReacoes(alertaId);
    }

    /** Retorna o total de likes e dislikes de um alerta. */
    public Map<String, Long> obterContadoresReacoes(Long alertaId) {
        long likes    = reacaoRepository.countByAlertaIdAndTipo(alertaId, AlertaReacao.TipoReacao.LIKE);
        long dislikes = reacaoRepository.countByAlertaIdAndTipo(alertaId, AlertaReacao.TipoReacao.DISLIKE);
        return Map.of("likes", likes, "dislikes", dislikes);
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
        long likes    = reacaoRepository.countByAlertaIdAndTipo(alerta.getId(), AlertaReacao.TipoReacao.LIKE);
        long dislikes = reacaoRepository.countByAlertaIdAndTipo(alerta.getId(), AlertaReacao.TipoReacao.DISLIKE);
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
            meuAlerta,
            likes,
            dislikes
        );
    }
}