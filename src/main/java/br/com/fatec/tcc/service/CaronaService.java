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
import br.com.fatec.tcc.repository.ParticipacaoCaronaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CaronaService {

    private final CaronaRepository caronaRepository;
    private final ParticipacaoCaronaRepository participacaoRepository;
    private final AvaliacaoCaronaRepository avaliacaoRepository;
    private final DenunciaCaronaRepository denunciaRepository;
    private final UsuarioService usuarioService;

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
                .map(c -> convertToResponseDTO(c, usuarioLogado))
                .collect(Collectors.toList());
    }

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

        // Verifica se todas as vagas foram preenchidas
        long totalConfirmados = participacaoRepository.countByCaronaAndStatus(carona,
                ParticipacaoCarona.StatusParticipacao.CONFIRMADA);
        if (totalConfirmados >= carona.getVagasDisponiveis()) {
            carona.setStatus(Carona.StatusCarona.CHEIA);
            caronaRepository.save(carona);
        }
    }

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
        caronaRepository.save(carona);
    }

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
            caronaRepository.delete(carona);
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

    public CaronaResponseDTO buscarPorId(Long id) {
        Carona carona = caronaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
        return convertToResponseDTO(carona);
    }

    public List<CaronaResponseDTO> listarCaronasPorUsuario(Usuario usuario) {
        List<CaronaResponseDTO> oferecidas = caronaRepository.findByMotoristaOrderByDataCriacaoDesc(usuario)
                .stream().map(this::convertToResponseDTO).collect(Collectors.toList());
        List<CaronaResponseDTO> solicitadas = participacaoRepository.findByPassageiroOrderByDataSolicitacaoDesc(usuario)
                .stream().map(p -> convertToResponseDTO(p.getCarona())).collect(Collectors.toList());
        oferecidas.addAll(solicitadas);
        return oferecidas;
    }

    private CaronaResponseDTO convertToResponseDTO(Carona carona) {
        return convertToResponseDTO(carona, null);
    }

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

    private ParticipacaoCaronaDTO convertToDTO(ParticipacaoCarona p) {
        return new ParticipacaoCaronaDTO(
                p.getId(),
                p.getPassageiro().getNomeCompleto(),
                p.getPassageiro().getEmail(),
                p.getStatus()
        );
    }
}