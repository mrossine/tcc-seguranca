package br.com.fatec.tcc.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fatec.tcc.dto.CaronaRequestDTO;
import br.com.fatec.tcc.dto.CaronaResponseDTO;
import br.com.fatec.tcc.dto.DenunciaAdminDTO;
import br.com.fatec.tcc.dto.DenunciaRequestDTO;
import br.com.fatec.tcc.dto.ParticipacaoCaronaDTO;
import br.com.fatec.tcc.model.AvaliacaoCarona;
import br.com.fatec.tcc.model.Carona;
import br.com.fatec.tcc.model.DenunciaCarona;
import br.com.fatec.tcc.model.ParticipacaoCarona;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.repository.AvaliacaoCaronaRepository;
import br.com.fatec.tcc.repository.CaronaRepository;
import br.com.fatec.tcc.repository.DenunciaCaronaRepository;
import br.com.fatec.tcc.repository.MensagemCaronaRepository;
import br.com.fatec.tcc.repository.ParticipacaoCaronaRepository;
import lombok.RequiredArgsConstructor;

/**
 * Regras de negócio das CARONAS solidárias.
 *
 * Concentra todo o ciclo de vida da carona: oferta, solicitação/aceite de vagas,
 * finalização, cancelamento/exclusão, avaliação por estrelas e denúncias.
 * Também converte as entidades nos DTOs usados pelas telas.
 */
@Service
@RequiredArgsConstructor
public class CaronaService {

    private final CaronaRepository caronaRepository;
    private final ParticipacaoCaronaRepository participacaoRepository;
    private final AvaliacaoCaronaRepository avaliacaoRepository;
    private final DenunciaCaronaRepository denunciaRepository;
    private final MensagemCaronaRepository mensagemRepository;
    private final UsuarioService usuarioService;
    private final MensagemCaronaService mensagemCaronaService;

    /**
     * Cria/oferece uma nova carona (INSERÇÃO).
     * Valida a antecedência mínima de 30 min e bloqueia motoristas com mais de
     * 10 caronas e média de avaliação abaixo de 3,0 estrelas.
     */
    @Transactional
    public CaronaResponseDTO oferecerCarona(CaronaRequestDTO request, String email) {
        if (request.getHorarioSaida() == null ||
                request.getHorarioSaida().isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new RuntimeException("O horário de saída deve ter pelo menos 30 minutos de antecedência.");
        }

        Usuario motorista = usuarioService.findUserByUsername(email);

        // Verifica bloqueio: mais de 10 caronas criadas E média < 3 estrelas
        long totalCaronasCriadas = caronaRepository.countByMotorista(motorista);
        if (totalCaronasCriadas > 10) {
            Double media = avaliacaoRepository.calcularMediaMotorista(motorista);
            if (media != null && media < 3.0) {
                throw new RuntimeException(
                    "Você não pode criar novas caronas pois sua média de avaliação (%.1f ★) está abaixo de 3,0."
                    .formatted(media));
            }
        }

        Carona carona = new Carona();
        carona.setMotorista(motorista);
        carona.setOrigem(request.getOrigem());
        carona.setDestino(request.getDestino());
        carona.setHorarioSaida(request.getHorarioSaida());
        carona.setVagasDisponiveis(request.getVagasDisponiveis());
        carona.setVeiculoModelo(request.getVeiculoModelo());
        carona.setVeiculoPlaca(request.getVeiculoPlaca());
        carona.setObservacoes(request.getObservacoes());
        carona.setDestinoLatitude(request.getDestinoLatitude());
        carona.setDestinoLongitude(request.getDestinoLongitude());
        carona.setStatus(Carona.StatusCarona.ABERTA);

        Carona saved = caronaRepository.save(carona);
        return convertToResponseDTO(saved);
    }

    /**
     * Lista (CONSULTA) as caronas que o usuário pode ver: as abertas (com filtros
     * opcionais de origem/destino/horário) somadas às caronas privadas das quais ele participa.
     */
    @Transactional(readOnly = true)
    public List<CaronaResponseDTO> listarCaronasDisponiveis(String email, String origem, String destino,
                                                            LocalDateTime horarioInicio,
                                                            LocalDateTime horarioFim) {
        Usuario usuarioLogado = usuarioService.findUserByUsername(email);
        List<Carona> abertas = caronaRepository
                .buscarCaronasDisponiveis(LocalDateTime.now(), origem, destino, horarioInicio, horarioFim);
        List<Carona> privadas = caronaRepository.buscarCaronasPrivadasDoUsuario(email);

        List<Carona> todas = new java.util.ArrayList<>(abertas);
        todas.addAll(privadas);
        return todas.stream()
                .filter(c -> caronaVisivelNaLista(c, usuarioLogado))
                .map(c -> convertToResponseDTO(c, usuarioLogado))
                .collect(Collectors.toList());
    }

    /**
     * Define se uma carona deve aparecer na listagem geral:
     *  - ABERTA: visível a todos (já vem só de buscarCaronasDisponiveis);
     *  - CHEIA/FECHADA/COMPLETADA: visível apenas aos participantes (já vem da consulta privada);
     *  - CANCELADA: nunca aparece;
     *  - FINALIZADA: aparece por até 72h após a finalização, e some assim que a pendência do
     *    usuário acaba — passageiro deixa de ver após avaliar; motorista, após não ter mais o que
     *    denunciar. Após 72h some para todos (a avaliação continua disponível pelo perfil).
     */
    private boolean caronaVisivelNaLista(Carona carona, Usuario usuarioLogado) {
        Carona.StatusCarona status = carona.getStatus();
        if (status == Carona.StatusCarona.CANCELADA) return false;
        if (status != Carona.StatusCarona.FINALIZADA) return true;

        LocalDateTime referencia = carona.getDataFinalizacao() != null
                ? carona.getDataFinalizacao() : carona.getHorarioSaida();
        if (referencia != null && referencia.isBefore(LocalDateTime.now().minusHours(72))) {
            return false; // passou da janela de 72h
        }

        boolean ehMotorista = carona.getMotorista().getId().equals(usuarioLogado.getId());
        if (ehMotorista) {
            long confirmados = participacaoRepository.countByCaronaAndStatus(
                    carona, ParticipacaoCarona.StatusParticipacao.CONFIRMADA);
            return confirmados > 0; // motorista vê enquanto pode denunciar passageiros
        }
        // Passageiro: vê enquanto ainda não avaliou
        var optP = participacaoRepository.findByCaronaAndPassageiro(carona, usuarioLogado);
        boolean confirmado = optP.isPresent()
                && optP.get().getStatus() == ParticipacaoCarona.StatusParticipacao.CONFIRMADA;
        return confirmado && !avaliacaoRepository.existsByCaronaAndPassageiro(carona, usuarioLogado);
    }

    /**
     * Passageiro solicita uma vaga (INSERÇÃO de participação).
     * Valida que a carona está ABERTA, que o usuário ainda não solicitou e que há vaga.
     */
    @Transactional
    public void solicitarVaga(Long caronaId, String email) {
        Usuario passageiro = usuarioService.findUserByUsername(email);
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));

        if (carona.getStatus() != Carona.StatusCarona.ABERTA) {
            throw new RuntimeException("Esta carona não está mais disponível");
        }
        if (participacaoRepository.findByCaronaAndPassageiro(carona, passageiro).isPresent()) {
            throw new RuntimeException("Você já solicitou esta carona");
        }
        long vagasOcupadas = participacaoRepository.countByCaronaAndStatus(carona,
                ParticipacaoCarona.StatusParticipacao.CONFIRMADA);
        if (vagasOcupadas >= carona.getVagasDisponiveis()) {
            throw new RuntimeException("Não há vagas disponíveis");
        }

        ParticipacaoCarona participacao = new ParticipacaoCarona();
        participacao.setCarona(carona);
        participacao.setPassageiro(passageiro);
        participacao.setStatus(ParticipacaoCarona.StatusParticipacao.SOLICITADA);
        participacaoRepository.save(participacao);
    }

    /**
     * Motorista aceita um passageiro (ALTERAÇÃO de status para CONFIRMADA).
     * Confere a permissão e as vagas; se lotar, muda a carona para CHEIA.
     */
    @Transactional
    public void aceitarPassageiro(Long caronaId, Long participacaoId, String emailMotorista) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        Usuario motorista = usuarioService.findUserByUsername(emailMotorista);
        if (!carona.getMotorista().getId().equals(motorista.getId())) {
            throw new RuntimeException("Apenas o motorista pode aceitar passageiros");
        }
        ParticipacaoCarona participacao = participacaoRepository.findById(participacaoId)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));
        if (!participacao.getCarona().getId().equals(caronaId)) {
            throw new RuntimeException("Solicitação não pertence a esta carona");
        }
        long vagasOcupadas = participacaoRepository.countByCaronaAndStatus(carona,
                ParticipacaoCarona.StatusParticipacao.CONFIRMADA);
        if (vagasOcupadas >= carona.getVagasDisponiveis()) {
            throw new RuntimeException("Não há vagas disponíveis");
        }
        participacao.setStatus(ParticipacaoCarona.StatusParticipacao.CONFIRMADA);
        participacao.setDataConfirmacao(LocalDateTime.now());
        participacaoRepository.save(participacao);

        // Mensagem automática do sistema no chat avisando que o passageiro entrou
        Usuario passageiro = participacao.getPassageiro();
        mensagemCaronaService.enviarMensagemSistema(carona, passageiro,
                passageiro.getNomeCompleto() + " entrou na carona.");

        // Verifica se todas as vagas foram preenchidas
        long totalConfirmados = participacaoRepository.countByCaronaAndStatus(carona,
                ParticipacaoCarona.StatusParticipacao.CONFIRMADA);
        if (totalConfirmados >= carona.getVagasDisponiveis()) {
            carona.setStatus(Carona.StatusCarona.CHEIA);
            caronaRepository.save(carona);
        }
    }

    /** Motorista recusa um passageiro (ALTERAÇÃO de status para RECUSADA). */
    @Transactional
    public void recusarPassageiro(Long caronaId, Long participacaoId, String emailMotorista) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        Usuario motorista = usuarioService.findUserByUsername(emailMotorista);
        if (!carona.getMotorista().getId().equals(motorista.getId())) {
            throw new RuntimeException("Apenas o motorista pode recusar passageiros");
        }
        ParticipacaoCarona participacao = participacaoRepository.findById(participacaoId)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));
        if (!participacao.getCarona().getId().equals(caronaId)) {
            throw new RuntimeException("Solicitação não pertence a esta carona");
        }
        participacao.setStatus(ParticipacaoCarona.StatusParticipacao.RECUSADA);
        participacaoRepository.save(participacao);
    }

    /**
     * Motorista finaliza manualmente a carona (ALTERAÇÃO de status para FINALIZADA).
     * Permite fechamento de qualquer status não-terminal (ABERTA, CHEIA, FECHADA, COMPLETADA).
     * Útil para encerrar caronas que não seguiram o fluxo automático de rastreamento.
     */
    @Transactional
    public void finalizarCaronaManualmente(Long caronaId, String emailMotorista) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        Usuario motorista = usuarioService.findUserByUsername(emailMotorista);
        if (!carona.getMotorista().getId().equals(motorista.getId())) {
            throw new RuntimeException("Apenas o motorista pode finalizar a carona");
        }
        if (carona.getStatus() == Carona.StatusCarona.FINALIZADA
                || carona.getStatus() == Carona.StatusCarona.CANCELADA) {
            throw new IllegalStateException("Carona já está finalizada ou cancelada");
        }
        carona.setStatus(Carona.StatusCarona.FINALIZADA);
        carona.setDataFinalizacao(LocalDateTime.now());
        caronaRepository.save(carona);
    }

    /**
     * Motorista finaliza a viagem (ALTERAÇÃO de status para FINALIZADA).
     * Só é permitido após o início (estado FECHADA ou COMPLETADA). A partir daí,
     * passageiros podem avaliar e ambos podem denunciar.
     */
    @Transactional
    public void finalizarCarona(Long caronaId, String emailMotorista) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        Usuario motorista = usuarioService.findUserByUsername(emailMotorista);
        if (!carona.getMotorista().getId().equals(motorista.getId())) {
            throw new RuntimeException("Apenas o motorista pode finalizar a carona");
        }
        if (carona.getStatus() != Carona.StatusCarona.FECHADA
                && carona.getStatus() != Carona.StatusCarona.COMPLETADA) {
            throw new RuntimeException("A carona só pode ser finalizada após o horário de início");
        }
        carona.setStatus(Carona.StatusCarona.FINALIZADA);
        carona.setDataFinalizacao(LocalDateTime.now());
        caronaRepository.save(carona);
    }

    /**
     * Motorista cancela a carona (ALTERAÇÃO de status para CANCELADA).
     * Também cancela as solicitações que ainda estavam pendentes.
     */
    @Transactional
    public void cancelarCarona(Long caronaId, String emailMotorista) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        Usuario motorista = usuarioService.findUserByUsername(emailMotorista);
        if (!carona.getMotorista().getId().equals(motorista.getId())) {
            throw new RuntimeException("Apenas o motorista pode cancelar a carona");
        }
        carona.setStatus(Carona.StatusCarona.CANCELADA);
        for (ParticipacaoCarona p : carona.getParticipacoes()) {
            if (p.getStatus() == ParticipacaoCarona.StatusParticipacao.SOLICITADA) {
                p.setStatus(ParticipacaoCarona.StatusParticipacao.CANCELADA);
            }
        }
        caronaRepository.save(carona);
    }

    /**
     * Exclui/cancela uma carona (EXCLUSÃO ou ALTERAÇÃO conforme o caso).
     * - Admin/Moderador (não-motorista): exclui de fato do banco.
     * - Motorista: a carona é apenas marcada como CANCELADA (preserva o histórico)
     *   e só é permitido se ela ainda não foi iniciada.
     */
    @Transactional
    public void excluirCarona(Long caronaId, String emailUsuario) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        Usuario usuario = usuarioService.findUserByUsername(emailUsuario);
        boolean isMotorista = carona.getMotorista().getId().equals(usuario.getId());
        boolean isAdminOuModerador = usuario.getRole() == Usuario.Role.ADMIN
                || usuario.getRole() == Usuario.Role.MODERATOR;
        if (!isMotorista && !isAdminOuModerador) {
            throw new RuntimeException("Sem permissão para excluir esta carona");
        }
        if (isAdminOuModerador && !isMotorista) {
            // Remove os registros dependentes antes (não há cascade nessas relações)
            mensagemRepository.deleteByCaronaId(caronaId);
            denunciaRepository.deleteByCarona(carona);
            avaliacaoRepository.deleteByCarona(carona);
            caronaRepository.delete(carona); // participações têm cascade ALL
            return;
        }
        // Motorista só pode cancelar se a carona ainda não começou
        if (carona.getStatus() == Carona.StatusCarona.FECHADA) {
            throw new RuntimeException("A carona já foi iniciada e não pode ser cancelada");
        }
        // Cancela em vez de excluir para preservar histórico
        carona.setStatus(Carona.StatusCarona.CANCELADA);
        for (ParticipacaoCarona p : carona.getParticipacoes()) {
            if (p.getStatus() == ParticipacaoCarona.StatusParticipacao.SOLICITADA
                    || p.getStatus() == ParticipacaoCarona.StatusParticipacao.CONFIRMADA) {
                p.setStatus(ParticipacaoCarona.StatusParticipacao.CANCELADA);
            }
        }
        caronaRepository.save(carona);
    }

    /**
     * Passageiro avalia a carona com estrelas (1-5).
     * Só é permitido após FINALIZADA e se ainda não avaliou.
     */
    @Transactional
    public void avaliarCarona(Long caronaId, String emailPassageiro, Integer estrelas, String comentario) {
        if (estrelas == null || estrelas < 1 || estrelas > 5) {
            throw new RuntimeException("A avaliação deve ser entre 1 e 5 estrelas.");
        }
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        if (carona.getStatus() != Carona.StatusCarona.FINALIZADA) {
            throw new RuntimeException("Só é possível avaliar caronas finalizadas.");
        }
        Usuario passageiro = usuarioService.findUserByUsername(emailPassageiro);

        // Verifica se o passageiro participou e foi confirmado
        ParticipacaoCarona participacao = participacaoRepository
                .findByCaronaAndPassageiro(carona, passageiro)
                .orElseThrow(() -> new RuntimeException("Você não participou desta carona."));
        if (participacao.getStatus() != ParticipacaoCarona.StatusParticipacao.CONFIRMADA) {
            throw new RuntimeException("Apenas passageiros confirmados podem avaliar.");
        }
        if (avaliacaoRepository.existsByCaronaAndPassageiro(carona, passageiro)) {
            throw new RuntimeException("Você já avaliou esta carona.");
        }

        AvaliacaoCarona avaliacao = new AvaliacaoCarona();
        avaliacao.setCarona(carona);
        avaliacao.setPassageiro(passageiro);
        avaliacao.setMotorista(carona.getMotorista());
        avaliacao.setEstrelas(estrelas);
        avaliacao.setComentario(comentario);
        avaliacaoRepository.save(avaliacao);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Denúncias
    // ─────────────────────────────────────────────────────────────────────────

    /** Passageiros confirmados de uma carona — usado pelo motorista ao denunciar. */
    @Transactional(readOnly = true)
    public List<ParticipacaoCaronaDTO> listarPassageirosConfirmados(Long caronaId, String emailMotorista) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        Usuario motorista = usuarioService.findUserByUsername(emailMotorista);
        if (!carona.getMotorista().getId().equals(motorista.getId())) {
            throw new RuntimeException("Apenas o motorista pode ver os passageiros desta carona");
        }
        return participacaoRepository.findByCarona(carona).stream()
                .filter(p -> p.getStatus() == ParticipacaoCarona.StatusParticipacao.CONFIRMADA)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Indica se o usuário pode acessar o chat e os participantes da carona:
     * é o motorista ou um passageiro CONFIRMADO. Usado para liberar a interface.
     */
    @Transactional(readOnly = true)
    public boolean podeAcessarChat(Long caronaId, String email) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        Usuario usuario = usuarioService.findUserByUsername(email);
        if (carona.getMotorista().getId().equals(usuario.getId())) return true;
        return participacaoRepository.findByCaronaAndPassageiro(carona, usuario)
                .map(p -> p.getStatus() == ParticipacaoCarona.StatusParticipacao.CONFIRMADA)
                .orElse(false);
    }

    /**
     * Lista todos os participantes da carona (motorista + passageiros confirmados).
     * Só acessível a quem participa da carona (motorista ou passageiro confirmado).
     */
    @Transactional(readOnly = true)
    public List<ParticipacaoCaronaDTO> listarParticipantes(Long caronaId, String email) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        if (!podeAcessarChat(caronaId, email)) {
            throw new RuntimeException("Apenas participantes confirmados podem ver os participantes desta carona");
        }
        Usuario motorista = carona.getMotorista();
        List<ParticipacaoCaronaDTO> lista = new java.util.ArrayList<>();
        // Motorista aparece primeiro, com o status CONFIRMADA por convenção
        lista.add(new ParticipacaoCaronaDTO(
                null,
                motorista.getNomeCompleto() + " (motorista)",
                motorista.getEmail(),
                ParticipacaoCarona.StatusParticipacao.CONFIRMADA));
        participacaoRepository.findByCarona(carona).stream()
                .filter(p -> p.getStatus() == ParticipacaoCarona.StatusParticipacao.CONFIRMADA)
                .map(this::convertToDTO)
                .forEach(lista::add);
        return lista;
    }

    /**
     * Registra uma denúncia. O papel (passageiro x motorista) é determinado pelo
     * servidor: se o usuário logado é o motorista da carona, segue o fluxo de motorista;
     * caso contrário, é tratado como passageiro.
     */
    @Transactional
    public void denunciar(Long caronaId, String emailUsuario, DenunciaRequestDTO req) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        if (carona.getStatus() != Carona.StatusCarona.FINALIZADA) {
            throw new RuntimeException("Só é possível denunciar caronas finalizadas.");
        }
        Usuario usuario = usuarioService.findUserByUsername(emailUsuario);

        if (carona.getMotorista().getId().equals(usuario.getId())) {
            denunciarComoMotorista(carona, usuario, req);
        } else {
            denunciarComoPassageiro(carona, usuario, req);
        }
    }

    /** Passageiro confirmado denuncia o motorista da carona. */
    private void denunciarComoPassageiro(Carona carona, Usuario passageiro, DenunciaRequestDTO req) {
        DenunciaCarona.CategoriaDenuncia categoria = parseCategoria(req.categoria());
        validarDescricao(req.descricao());

        ParticipacaoCarona participacao = participacaoRepository
                .findByCaronaAndPassageiro(carona, passageiro)
                .orElseThrow(() -> new RuntimeException("Você não participou desta carona."));
        if (participacao.getStatus() != ParticipacaoCarona.StatusParticipacao.CONFIRMADA) {
            throw new RuntimeException("Apenas passageiros confirmados podem denunciar.");
        }

        Usuario motorista = carona.getMotorista();
        if (denunciaRepository.existsByCaronaAndDenuncianteAndDenunciado(carona, passageiro, motorista)) {
            throw new RuntimeException("Você já registrou uma denúncia para esta carona.");
        }
        salvarDenuncia(carona, passageiro, motorista,
                DenunciaCarona.TipoDenunciante.PASSAGEIRO, categoria, req.descricao());
    }

    /**
     * Motorista denuncia um passageiro específico (alvoEmail) ou a carona inteira
     * (todaCarona = true), gerando uma denúncia para cada passageiro confirmado.
     */
    private void denunciarComoMotorista(Carona carona, Usuario motorista, DenunciaRequestDTO req) {
        DenunciaCarona.CategoriaDenuncia categoria = parseCategoria(req.categoria());
        validarDescricao(req.descricao());

        List<ParticipacaoCarona> confirmados = participacaoRepository.findByCarona(carona).stream()
                .filter(p -> p.getStatus() == ParticipacaoCarona.StatusParticipacao.CONFIRMADA)
                .collect(Collectors.toList());

        boolean todaCarona = Boolean.TRUE.equals(req.todaCarona());

        if (todaCarona) {
            if (confirmados.isEmpty()) {
                throw new RuntimeException("Esta carona não teve passageiros confirmados.");
            }
            int criadas = 0;
            for (ParticipacaoCarona p : confirmados) {
                Usuario passageiro = p.getPassageiro();
                if (!denunciaRepository.existsByCaronaAndDenuncianteAndDenunciado(carona, motorista, passageiro)) {
                    salvarDenuncia(carona, motorista, passageiro,
                            DenunciaCarona.TipoDenunciante.MOTORISTA, categoria, req.descricao());
                    criadas++;
                }
            }
            if (criadas == 0) {
                throw new RuntimeException("Você já denunciou todos os passageiros desta carona.");
            }
        } else {
            if (req.alvoEmail() == null || req.alvoEmail().isBlank()) {
                throw new RuntimeException("Selecione o passageiro a ser denunciado.");
            }
            Usuario alvo = usuarioService.findUserByUsername(req.alvoEmail());
            boolean ehConfirmado = confirmados.stream()
                    .anyMatch(p -> p.getPassageiro().getId().equals(alvo.getId()));
            if (!ehConfirmado) {
                throw new RuntimeException("O passageiro informado não participou desta carona.");
            }
            if (denunciaRepository.existsByCaronaAndDenuncianteAndDenunciado(carona, motorista, alvo)) {
                throw new RuntimeException("Você já denunciou este passageiro nesta carona.");
            }
            salvarDenuncia(carona, motorista, alvo,
                    DenunciaCarona.TipoDenunciante.MOTORISTA, categoria, req.descricao());
        }
    }

    private void salvarDenuncia(Carona carona, Usuario denunciante, Usuario denunciado,
                                DenunciaCarona.TipoDenunciante tipo,
                                DenunciaCarona.CategoriaDenuncia categoria, String descricao) {
        DenunciaCarona d = new DenunciaCarona();
        d.setCarona(carona);
        d.setDenunciante(denunciante);
        d.setDenunciado(denunciado);
        d.setTipoDenunciante(tipo);
        d.setCategoria(categoria);
        d.setDescricao(descricao.trim());
        d.setStatus(DenunciaCarona.StatusDenuncia.PENDENTE);
        denunciaRepository.save(d);
    }

    private DenunciaCarona.CategoriaDenuncia parseCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            throw new RuntimeException("Selecione a categoria da denúncia.");
        }
        try {
            return DenunciaCarona.CategoriaDenuncia.valueOf(categoria.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Categoria de denúncia inválida.");
        }
    }

    private void validarDescricao(String descricao) {
        if (descricao == null || descricao.trim().length() < 5) {
            throw new RuntimeException("Descreva o ocorrido com pelo menos 5 caracteres.");
        }
        if (descricao.trim().length() > 1000) {
            throw new RuntimeException("A descrição não pode passar de 1000 caracteres.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Denúncias — administração
    // ─────────────────────────────────────────────────────────────────────────

    /** Lista todas as denúncias (opcionalmente filtradas por status) para o admin. */
    public List<DenunciaAdminDTO> listarDenuncias(String statusFiltro) {
        List<DenunciaCarona> lista;
        if (statusFiltro == null || statusFiltro.isBlank()) {
            lista = denunciaRepository.findAllByOrderByDataDenunciaDesc();
        } else {
            DenunciaCarona.StatusDenuncia status;
            try {
                status = DenunciaCarona.StatusDenuncia.valueOf(statusFiltro.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Status inválido.");
            }
            lista = denunciaRepository.findByStatusOrderByDataDenunciaDesc(status);
        }
        return lista.stream().map(this::toDenunciaAdminDTO).collect(Collectors.toList());
    }

    /** Atualiza o status de uma denúncia (PENDENTE, EM_ANALISE, RESOLVIDA, ARQUIVADA). */
    @Transactional
    public void atualizarStatusDenuncia(Long denunciaId, String novoStatus) {
        DenunciaCarona d = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new RuntimeException("Denúncia não encontrada"));
        if (novoStatus == null || novoStatus.isBlank()) {
            throw new RuntimeException("Informe o novo status.");
        }
        try {
            d.setStatus(DenunciaCarona.StatusDenuncia.valueOf(novoStatus.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Status inválido.");
        }
        denunciaRepository.save(d);
    }

    private DenunciaAdminDTO toDenunciaAdminDTO(DenunciaCarona d) {
        Carona c = d.getCarona();
        return new DenunciaAdminDTO(
                d.getId(),
                c.getId(),
                c.getOrigem(),
                c.getDestino(),
                c.getHorarioSaida(),
                d.getTipoDenunciante().name(),
                d.getDenunciante().getNomeCompleto(),
                d.getDenunciante().getEmail(),
                d.getDenunciado().getNomeCompleto(),
                d.getDenunciado().getEmail(),
                d.getCategoria().name(),
                d.getDescricao(),
                d.getStatus().name(),
                d.getDataDenuncia()
        );
    }

    /** Lista (CONSULTA) as solicitações ainda pendentes de uma carona — visível só ao motorista. */
    @Transactional(readOnly = true)
    public List<ParticipacaoCaronaDTO> listarSolicitacoesPorCarona(Long caronaId, String emailMotorista) {
        Carona carona = caronaRepository.findById(caronaId)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        Usuario motorista = usuarioService.findUserByUsername(emailMotorista);
        if (!carona.getMotorista().getId().equals(motorista.getId())) {
            throw new RuntimeException("Apenas o motorista pode ver as solicitações");
        }
        return carona.getParticipacoes().stream()
                .filter(p -> p.getStatus() == ParticipacaoCarona.StatusParticipacao.SOLICITADA)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca uma carona por ID verificando se o usuário tem permissão de visualizá-la.
     * - ABERTA: qualquer usuário autenticado pode ver (precisa ver detalhes para solicitar).
     * - CHEIA/FECHADA/COMPLETADA/FINALIZADA: apenas motorista, passageiros confirmados e admins.
     * - CANCELADA: apenas motorista e admins.
     */
    @Transactional(readOnly = true)
    public CaronaResponseDTO buscarPorId(Long id, String email) {
        Carona carona = caronaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        Usuario usuario = usuarioService.findUserByUsername(email);

        if (usuario.getRole() == Usuario.Role.ADMIN) {
            return convertToResponseDTO(carona, usuario);
        }

        Carona.StatusCarona status = carona.getStatus();
        boolean ehMotorista = carona.getMotorista().getId().equals(usuario.getId());

        if (status == Carona.StatusCarona.ABERTA) {
            return convertToResponseDTO(carona, usuario);
        }
        if (ehMotorista) {
            return convertToResponseDTO(carona, usuario);
        }
        boolean ehPassageiroConfirmado = participacaoRepository
                .findByCaronaAndPassageiro(carona, usuario)
                .map(p -> p.getStatus() == ParticipacaoCarona.StatusParticipacao.CONFIRMADA)
                .orElse(false);
        if (ehPassageiroConfirmado) {
            return convertToResponseDTO(carona, usuario);
        }
        throw new RuntimeException("Acesso negado a esta carona");
    }

    /** Lista (CONSULTA) as caronas ligadas ao usuário: as que ele ofereceu e as que solicitou. */
    @Transactional(readOnly = true)
    public List<CaronaResponseDTO> listarCaronasPorUsuario(Usuario usuario) {
        List<CaronaResponseDTO> oferecidas = caronaRepository.findByMotoristaOrderByDataCriacaoDesc(usuario)
                .stream().map(this::convertToResponseDTO).collect(Collectors.toList());
        List<CaronaResponseDTO> solicitadas = participacaoRepository.findByPassageiroOrderByDataSolicitacaoDesc(usuario)
                .stream().map(p -> convertToResponseDTO(p.getCarona())).collect(Collectors.toList());
        oferecidas.addAll(solicitadas);
        return oferecidas;
    }

    /**
     * Lista (CONSULTA) as caronas em que o usuário foi PASSAGEIRO confirmado.
     * Usado no histórico do perfil para oferecer a avaliação das caronas finalizadas.
     */
    public List<CaronaResponseDTO> listarCaronasComoPassageiro(Usuario usuario) {
        return participacaoRepository.findByPassageiroOrderByDataSolicitacaoDesc(usuario).stream()
                .filter(p -> p.getStatus() == ParticipacaoCarona.StatusParticipacao.CONFIRMADA)
                .map(p -> convertToResponseDTO(p.getCarona(), usuario))
                .collect(Collectors.toList());
    }

    /** Conversão simples (sem usuário logado): não calcula permissões de avaliar/denunciar. */
    private CaronaResponseDTO convertToResponseDTO(Carona carona) {
        return convertToResponseDTO(carona, null);
    }

    /**
     * Converte a entidade Carona no DTO da tela, calculando campos derivados:
     *  - vagas ocupadas e média de avaliação do motorista (exibida após 10+ caronas);
     *  - podeAvaliar : passageiro confirmado, carona FINALIZADA e ainda não avaliou;
     *  - podeDenunciar : após FINALIZADA, motorista (com passageiros) ou passageiro confirmado.
     */
    private CaronaResponseDTO convertToResponseDTO(Carona carona, Usuario usuarioLogado) {
        long vagasOcupadas = participacaoRepository.countByCaronaAndStatus(carona,
                ParticipacaoCarona.StatusParticipacao.CONFIRMADA);

        Usuario motorista = carona.getMotorista();
        long totalCaronasCriadas = caronaRepository.countByMotorista(motorista);

        // Média só é exibida após mais de 10 caronas criadas
        Double media = null;
        long totalAvaliacoes = avaliacaoRepository.countByMotorista(motorista);
        if (totalCaronasCriadas > 10) {
            media = avaliacaoRepository.calcularMediaMotorista(motorista);
        }

        // Pode avaliar: carona finalizada + passageiro confirmado + ainda não avaliou
        boolean podeAvaliar = false;
        // Pode denunciar: carona finalizada + (motorista com passageiros confirmados
        //                 OU passageiro confirmado)
        boolean podeDenunciar = false;
        if (usuarioLogado != null
                && carona.getStatus() == Carona.StatusCarona.FINALIZADA) {
            boolean ehMotorista = motorista.getId().equals(usuarioLogado.getId());
            if (ehMotorista) {
                long confirmados = participacaoRepository.countByCaronaAndStatus(
                        carona, ParticipacaoCarona.StatusParticipacao.CONFIRMADA);
                podeDenunciar = confirmados > 0;
            } else {
                var optP = participacaoRepository.findByCaronaAndPassageiro(carona, usuarioLogado);
                boolean confirmado = optP.isPresent()
                        && optP.get().getStatus() == ParticipacaoCarona.StatusParticipacao.CONFIRMADA;
                podeDenunciar = confirmado;
                if (confirmado && !avaliacaoRepository.existsByCaronaAndPassageiro(carona, usuarioLogado)) {
                    podeAvaliar = true;
                }
            }
        }

        return new CaronaResponseDTO(
                carona.getId(),
                motorista.getNomeCompleto(),
                motorista.getEmail(),
                carona.getOrigem(),
                carona.getDestino(),
                carona.getHorarioSaida(),
                carona.getVagasDisponiveis(),
                (int) vagasOcupadas,
                carona.getVeiculoModelo(),
                carona.getVeiculoPlaca(),
                carona.getObservacoes(),
                carona.getStatus(),
                media != null ? Math.round(media * 10.0) / 10.0 : null,
                totalAvaliacoes,
                podeAvaliar,
                podeDenunciar
        );
    }

    /** Converte uma participação (passageiro + status) no DTO usado nas listas. */
    private ParticipacaoCaronaDTO convertToDTO(ParticipacaoCarona p) {
        return new ParticipacaoCaronaDTO(
                p.getId(),
                p.getPassageiro().getNomeCompleto(),
                p.getPassageiro().getEmail(),
                p.getStatus()
        );
    }
}